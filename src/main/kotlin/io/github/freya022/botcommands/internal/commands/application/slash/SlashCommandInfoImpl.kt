package io.github.freya022.botcommands.internal.commands.application.slash

import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.INamedCommand
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.SlashCommandInfo
import io.github.freya022.botcommands.api.commands.application.slash.builder.SlashCommandBuilder
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.utils.simpleNestedName
import io.github.freya022.botcommands.internal.*
import io.github.freya022.botcommands.internal.commands.application.ApplicationCommandInfoImpl
import io.github.freya022.botcommands.internal.commands.application.ApplicationGeneratedOption
import io.github.freya022.botcommands.internal.commands.application.slash.SlashUtils.getCheckedDefaultValue
import io.github.freya022.botcommands.internal.core.options.OptionImpl
import io.github.freya022.botcommands.internal.core.options.OptionType
import io.github.freya022.botcommands.internal.core.reflection.toMemberParamFunction
import io.github.freya022.botcommands.internal.parameters.CustomMethodOption
import io.github.freya022.botcommands.internal.parameters.ServiceMethodOption
import io.github.freya022.botcommands.internal.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.jvm.jvmErasure

private val logger = KotlinLogging.logger { }

internal sealed class SlashCommandInfoImpl(
    final override val context: BContext,
    private val _topLevelInstance: TopLevelSlashCommandInfoImpl?,
    private val _parentInstance: INamedCommand?,
    builder: SlashCommandBuilder
) : ApplicationCommandInfoImpl(builder),
    SlashCommandInfo {

    override val topLevelInstance: TopLevelSlashCommandInfoImpl
        get() = _topLevelInstance ?: throwInternal("This should have been overridden or not been null")
    override val parentInstance: INamedCommand?
        get() = _parentInstance ?: throwInternal("This should have been overridden")

    final override val description: String

    final override val eventFunction = builder.toMemberParamFunction<GlobalSlashEvent, _>(context)
    final override val parameters: List<SlashCommandParameterImpl>

    init {
        eventFunction.checkEventScope<GuildSlashEvent>(builder)

        description = LocalizationUtils.getCommandDescription(context, builder, builder.description)

        parameters = builder.optionAggregateBuilders.transform {
            SlashCommandParameterImpl(context, this@SlashCommandInfoImpl, builder, builder.optionAggregateBuilders, it)
        }

        parameters
            .flatMap { it.allOptions }
            .filterIsInstance<SlashCommandOptionImpl>()
            .forEach(SlashCommandOptionImpl::buildAutocomplete)
    }

    internal suspend fun execute(event: GlobalSlashEvent): Boolean {
        val objects = getSlashOptions(event, parameters) ?: return false
        function.callSuspendBy(objects)

        return true
    }

    context(ExecutableMixin)
    internal suspend fun <T> getSlashOptions(
        event: T,
        parameters: List<AbstractSlashCommandParameter>
    ): Map<KParameter, Any?>? where T : CommandInteractionPayload, T : Event {
        val optionValues = parameters.mapOptions { option ->
            if (tryInsertOption(event, this, option) == InsertOptionResult.ABORT)
                return null
        }

        return parameters.mapFinalParameters(event, optionValues)
    }

    private suspend fun <T> tryInsertOption(
        event: T,
        optionMap: MutableMap<OptionImpl, Any?>,
        option: OptionImpl
    ): InsertOptionResult where T : CommandInteractionPayload,
                                T : Event {
        val value = when (option.optionType) {
            OptionType.OPTION -> {
                option as AbstractSlashCommandOption

                val optionName = option.discordName
                val optionMapping = event.getOption(optionName)

                if (optionMapping != null) {
                    val resolved = option.resolver.resolveSuspend(this, event, optionMapping)
                    if (resolved == null) {
                        //Not a warning, could be normal if the user did not supply a valid string for user-defined resolvers
                        logger.trace {
                            "The parameter '${option.declaredName}' of value '${optionMapping.asString}' could not be resolved into a ${option.type.jvmErasure.simpleNestedName}"
                        }

                        return when {
                            option.isOptionalOrNullable || option.isVararg -> InsertOptionResult.SKIP
                            else -> {
                                //Only use the generic message if the user didn't handle this situation
                                if (!event.isAcknowledged && event is SlashCommandInteractionEvent) {
                                    event.reply_(
                                        context.getDefaultMessages(event).getSlashCommandUnresolvableOptionMsg(option.discordName),
                                        ephemeral = true
                                    ).queue()
                                }

                                InsertOptionResult.ABORT
                            }
                        }
                    }

                    resolved
                } else if (option.isRequired && event is CommandAutoCompleteInteractionEvent) {
                    return InsertOptionResult.ABORT
                } else {
                    null
                }
            }
            OptionType.CUSTOM -> {
                option as CustomMethodOption

                option.resolver.resolveSuspend(this, event)
            }
            OptionType.GENERATED -> {
                option as ApplicationGeneratedOption

                option.getCheckedDefaultValue { it.generatedValueSupplier.getDefaultValue(event) }
            }
            OptionType.SERVICE -> (option as ServiceMethodOption).getService()
            OptionType.CONSTANT -> throwInternal("${option.optionType} has not been implemented")
        }

        return tryInsertNullableOption(value, option, optionMap)
    }
}