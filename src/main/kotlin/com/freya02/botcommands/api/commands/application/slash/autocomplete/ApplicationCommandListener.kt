package com.freya02.botcommands.api.commands.application.slash.autocomplete

import com.freya02.botcommands.api.commands.CommandPath
import com.freya02.botcommands.api.commands.CooldownScope
import com.freya02.botcommands.api.commands.application.ApplicationFilteringData
import com.freya02.botcommands.api.core.annotations.BEventListener
import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.internal.BContextImpl
import com.freya02.botcommands.internal.Usability
import com.freya02.botcommands.internal.Usability.UnusableReason
import com.freya02.botcommands.internal.commands.application.ApplicationCommandInfo
import com.freya02.botcommands.internal.core.CooldownService
import com.freya02.botcommands.internal.core.ExceptionHandler
import com.freya02.botcommands.internal.core.ExceptionHandlerBuilder
import com.freya02.botcommands.internal.throwInternal
import com.freya02.botcommands.internal.throwUser
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.CommandInteraction
import java.util.*

@BService
internal class ApplicationCommandListener(private val context: BContextImpl, private val cooldownService: CooldownService, private val exceptionHandler: ExceptionHandler) {
    private val logger = KotlinLogging.logger {  }

    @BEventListener
    suspend fun onSlashCommand(event: SlashCommandInteractionEvent) {
        logger.trace { "Received slash command: ${reconstructCommand(event)}" }

        exceptionHandler.runCatching(event, { configureHandler(event) }) {
            val slashCommand = CommandPath.of(event.commandPath).let {
                context.applicationCommandsContext.findLiveSlashCommand(event.guild, it)
                    ?: context.applicationCommandsContext.findLiveSlashCommand(null, it)
                    ?: throwUser("A slash command could not be found: ${event.commandPath}")
            }

            if (!canRun(event, slashCommand)) return
            withContext(context.config.coroutineScopesConfig.applicationCommandsScope.coroutineContext) {
                slashCommand.execute(event, cooldownService)
            }
        }
    }

    @BEventListener
    suspend fun onUserContextCommand(event: UserContextInteractionEvent) {
        logger.trace { "Received user context command: ${reconstructCommand(event)}" }

        exceptionHandler.runCatching(event, { configureHandler(event) }) {
            val userCommand = event.name.let {
                context.applicationCommandsContext.findLiveUserCommand(event.guild, it)
                    ?: context.applicationCommandsContext.findLiveUserCommand(null, it)
                    ?: throwUser("A user context command could not be found: ${event.commandPath}")
            }

            if (!canRun(event, userCommand)) return
            withContext(context.config.coroutineScopesConfig.applicationCommandsScope.coroutineContext) {
                userCommand.execute(context, cooldownService, event)
            }
        }
    }

    @BEventListener
    suspend fun onMessageContextCommand(event: MessageContextInteractionEvent) {
        logger.trace { "Received message context command: ${reconstructCommand(event)}" }

        exceptionHandler.runCatching(event, { configureHandler(event) }) {
            val messageCommand = event.name.let {
                context.applicationCommandsContext.findLiveMessageCommand(event.guild, it)
                    ?: context.applicationCommandsContext.findLiveMessageCommand(null, it)
                    ?: throwUser("A message context command could not be found: ${event.commandPath}")
            }

            if (!canRun(event, messageCommand)) return
            withContext(context.config.coroutineScopesConfig.applicationCommandsScope.coroutineContext) {
                messageCommand.execute(context, cooldownService, event)
            }
        }
    }

    private fun ExceptionHandlerBuilder.configureHandler(event: GenericCommandInteractionEvent) {
        logMessage = { "Unhandled exception while executing an application command '${reconstructCommand(event)}'" }
        dispatchMessage = { "Exception in application command '${reconstructCommand(event)}'" }
        postRun = {
            val generalErrorMsg = context.getDefaultMessages(event).generalErrorMsg
            when {
                event.isAcknowledged -> event.hook.sendMessage(generalErrorMsg).setEphemeral(true).queue()
                else -> event.reply(generalErrorMsg).setEphemeral(true).queue()
            }
        }
    }

    private fun canRun(event: GenericCommandInteractionEvent, applicationCommand: ApplicationCommandInfo): Boolean {
        val applicationFilteringData = ApplicationFilteringData(context, event, applicationCommand)
        for (applicationFilter in context.config.applicationConfig.applicationFilters) {
            if (!applicationFilter.isAccepted(applicationFilteringData)) {
                logger.trace("Cancelled application commands due to filter")
                return false
            }
        }

        val isNotOwner = !context.isOwner(event.user.idLong)
        val usability = Usability.of(context, event, applicationCommand, isNotOwner)
        if (usability.isUnusable) {
            val unusableReasons = usability.unusableReasons
            when {
                UnusableReason.OWNER_ONLY in unusableReasons -> {
                    reply(event, context.getDefaultMessages(event).ownerOnlyErrorMsg)
                    return false
                }
                UnusableReason.NSFW_DISABLED in unusableReasons -> {
                    reply(event, context.getDefaultMessages(event).nsfwDisabledErrorMsg)
                    return false
                }
                UnusableReason.NSFW_ONLY in unusableReasons -> {
                    reply(event, context.getDefaultMessages(event).nsfwOnlyErrorMsg)
                    return false
                }
                UnusableReason.NSFW_DM_DENIED in unusableReasons -> {
                    reply(event, context.getDefaultMessages(event).nsfwdmDeniedErrorMsg)
                    return false
                }
                UnusableReason.USER_PERMISSIONS in unusableReasons -> {
                    reply(event, context.getDefaultMessages(event).userPermErrorMsg)
                    return false
                }
                UnusableReason.BOT_PERMISSIONS in unusableReasons -> {
                    if (event.guild == null) throwInternal("BOT_PERMISSIONS got checked even if guild is null")
                    val missingBuilder = StringJoiner(", ")

                    //Take needed permissions, extract bot current permissions
                    val missingPerms = applicationCommand.botPermissions
                    missingPerms.removeAll(event.guild!!.selfMember.getPermissions(event.guildChannel))
                    for (botPermission in missingPerms) {
                        missingBuilder.add(botPermission.getName())
                    }
                    reply(event, context.getDefaultMessages(event).getBotPermErrorMsg(missingBuilder.toString()))

                    return false
                }
            }
        }

        if (isNotOwner) {
            val cooldown = cooldownService.getCooldown(applicationCommand, event)
            if (cooldown > 0) {
                val messages = context.getDefaultMessages(event)

                when (applicationCommand.cooldownStrategy.scope) {
                    CooldownScope.USER -> reply(event, messages.getUserCooldownMsg(cooldown / 1000.0))
                    CooldownScope.GUILD -> reply(event, messages.getGuildCooldownMsg(cooldown / 1000.0))
                    //Implicit CooldownScope.CHANNEL
                    else -> reply(event, messages.getChannelCooldownMsg(cooldown / 1000.0))
                }

                return false
            }
        }

        return true
    }

    private fun reply(event: CommandInteraction, msg: String) {
        event.reply(msg)
            .setEphemeral(true)
            .queue(null) {
                logger.error("Could not send reply message from application command listener", it)
                context.dispatchException("Could not send reply message from application command listener", it)
            }
    }

    private fun reconstructCommand(event: GenericCommandInteractionEvent): String {
        return when (event) {
            is SlashCommandInteractionEvent -> event.commandString
            else -> event.name
        }
    }
}