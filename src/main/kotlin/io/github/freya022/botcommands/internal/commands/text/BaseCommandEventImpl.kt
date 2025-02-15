package io.github.freya022.botcommands.internal.commands.text

import io.github.freya022.botcommands.api.commands.ratelimit.CancellableRateLimit
import io.github.freya022.botcommands.api.commands.text.BaseCommandEvent
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.utils.loggerOf
import io.github.freya022.botcommands.api.localization.text.LocalizableTextCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.api.Permission.MESSAGE_HISTORY
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.utils.FileUpload
import java.io.InputStream
import java.util.function.Consumer
import javax.annotation.CheckReturnValue

private val logger = KotlinLogging.loggerOf<BaseCommandEvent>()

private val SUCCESS = Emoji.fromUnicode("✅") // white_check_mark
private val ERROR = Emoji.fromUnicode("❌") // x

internal open class BaseCommandEventImpl(
    private val context: BContext,
    private val event: MessageReceivedEvent,
    arguments: String,
    cancellableRateLimit: CancellableRateLimit,
    localizableTextCommand: LocalizableTextCommand,
) : BaseCommandEvent(event.jda, event.responseNumber, event.message),
    CancellableRateLimit by cancellableRateLimit,
    LocalizableTextCommand by localizableTextCommand {

    private val argumentsStr: String = arguments

    override fun getContext(): BContext = context

    override fun getRawData() = event.rawData

    override fun getArgumentsStrList(): List<String> = when {
        argumentsStr.isNotBlank() -> argumentsStr.split(' ').dropLastWhile { it.isEmpty() }
        else -> listOf()
    }

    override fun getArgumentsStr(): String = argumentsStr

    override fun reportError(message: String, e: Throwable) {
        channel.sendMessage(message).queue(null) { t: Throwable? -> logger.error(t) { "Could not send message to channel : $message" } }
        context.dispatchException(message, e)
    }

    override fun failureReporter(message: String): Consumer<in Throwable> {
        return Consumer { t: Throwable -> reportError(message, t) }
    }

    override fun getAuthorBestName(): String {
        return member.effectiveName
    }

    override fun getDefaultEmbed(): EmbedBuilder {
        return context.textCommandsContext.defaultEmbedSupplier.get()
    }

    override fun getDefaultIconStream(): InputStream? = context.textCommandsContext.defaultEmbedFooterIconSupplier.get()

    override fun sendWithEmbedFooterIcon(embed: MessageEmbed): RestAction<Message> =
        sendWithEmbedFooterIcon(channel, embed)

    @CheckReturnValue
    override fun sendWithEmbedFooterIcon(
        channel: MessageChannel,
        embed: MessageEmbed
    ): RestAction<Message> = sendWithEmbedFooterIcon(channel, defaultIconStream, embed)

    @CheckReturnValue
    override fun sendWithEmbedFooterIcon(
        channel: MessageChannel,
        iconStream: InputStream?,
        embed: MessageEmbed
    ): RestAction<Message> = when {
        iconStream != null -> channel.sendTyping().flatMap { channel.sendFiles(FileUpload.fromData(iconStream, "icon.jpg")).setEmbeds(embed) }
        else -> channel.sendTyping().flatMap { channel.sendMessageEmbeds(embed) }
    }

    @CheckReturnValue
    override fun reactSuccess(): RestAction<Void> = channel.addReactionById(messageId, SUCCESS)

    @CheckReturnValue
    override fun reactError(): RestAction<Void> = channel.addReactionById(messageId, ERROR)

    override fun respond(text: CharSequence): MessageCreateAction = channel.sendMessage(text)

    override fun respondFormat(format: String, vararg args: Any): MessageCreateAction = channel.sendMessageFormat(format, *args)

    override fun respond(embed: MessageEmbed, vararg other: MessageEmbed): MessageCreateAction = channel.sendMessageEmbeds(embed, *other)

    override fun respondFile(vararg fileUploads: FileUpload): MessageCreateAction = channel.sendFiles(*fileUploads)

    @CheckReturnValue
    override fun reply(text: CharSequence): MessageCreateAction = message.reply(text)

    @CheckReturnValue
    override fun replyFormat(format: String, vararg args: Any): MessageCreateAction = message.replyFormat(format, *args)

    @CheckReturnValue
    override fun reply(embed: MessageEmbed, vararg other: MessageEmbed): MessageCreateAction = message.replyEmbeds(embed, *other)

    @CheckReturnValue
    override fun replyFile(vararg fileUploads: FileUpload): RestAction<Message> =
        channel.sendTyping().flatMap { message.replyFiles(*fileUploads) }

    override fun indicateError(text: CharSequence): RestAction<Message> = when {
        guild.selfMember.hasPermission(guildChannel, MESSAGE_ADD_REACTION, MESSAGE_HISTORY) -> reactError().flatMap { channel.sendMessage(text) }
        else -> channel.sendMessage(text)
    }

    override fun indicateErrorFormat(format: String, vararg args: Any): RestAction<Message> = when {
        guild.selfMember.hasPermission(guildChannel, MESSAGE_ADD_REACTION, MESSAGE_HISTORY) -> reactError().flatMap { channel.sendMessageFormat(format, *args) }
        else -> channel.sendMessageFormat(format, *args)
    }

    override fun indicateError(embed: MessageEmbed, vararg other: MessageEmbed): RestAction<Message> = when {
        guild.selfMember.hasPermission(guildChannel, MESSAGE_ADD_REACTION, MESSAGE_HISTORY) -> reactError().flatMap { channel.sendMessageEmbeds(embed, *other) }
        else -> channel.sendMessageEmbeds(embed, *other)
    }
}