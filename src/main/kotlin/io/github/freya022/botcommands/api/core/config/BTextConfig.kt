package io.github.freya022.botcommands.api.core.config

import io.github.freya022.botcommands.api.commands.text.IHelpCommand
import io.github.freya022.botcommands.api.commands.text.TextPrefixSupplier
import io.github.freya022.botcommands.api.commands.text.annotations.RequiresTextCommands
import io.github.freya022.botcommands.api.core.service.annotations.InjectedService
import io.github.freya022.botcommands.api.core.utils.toImmutableList
import io.github.freya022.botcommands.api.localization.DefaultMessages
import io.github.freya022.botcommands.internal.core.config.ConfigDSL
import io.github.freya022.botcommands.internal.core.config.ConfigurationValue
import net.dv8tion.jda.api.entities.emoji.Emoji

@InjectedService
interface BTextConfig {
    /**
     * Whether text commands should be listened for.
     *
     * You can use [@RequiresTextCommands][RequiresTextCommands]
     * to disable services when this is set to `false`.
     *
     * Default: `true`
     *
     * Spring property: `botcommands.text.enable`
     */
    @ConfigurationValue(path = "botcommands.text.enable", defaultValue = "true")
    val enable: Boolean

    /**
     * Whether the bot should look for commands when it is mentioned.
     *
     * This prefix is not always used for text command detection,
     * as it can be overridden by [TextPrefixSupplier], but you can read this property and return it
     * if, for example, the guild channel has no special prefix set.
     *
     * Default: `false`
     *
     * Spring property: `botcommands.text.usePingAsPrefix`
     */
    @ConfigurationValue(path = "botcommands.text.usePingAsPrefix", defaultValue = "false")
    val usePingAsPrefix: Boolean

    /**
     * Prefixes the bot should listen to.
     *
     * These prefixes are not always used for text command detection,
     * as they can be overridden by [TextPrefixSupplier], but you can read this property and return them
     * if, for example, the guild channel has no special prefix set.
     *
     * Spring property: `botcommands.text.prefixes`
     */
    @ConfigurationValue(path = "botcommands.text.prefixes")
    val prefixes: List<String>

    /**
     * Whether the default help command is disabled. This also disables help content when a user misuses a command.
     *
     * This still lets you define your own help command with [IHelpCommand].
     *
     * Default: `false`
     *
     * Spring property: `botcommands.text.isHelpDisabled`
     */
    @ConfigurationValue(path = "botcommands.text.isHelpDisabled", defaultValue = "false")
    val isHelpDisabled: Boolean

    /**
     * Whether command suggestions will be shown when a user tries to use an invalid command.
     *
     * Default: `true`
     *
     * Spring property: `botcommands.text.showSuggestions`
     */
    @ConfigurationValue(path = "botcommands.text.showSuggestions", defaultValue = "true")
    val showSuggestions: Boolean

    // 🐟 was also a strong candidate
    /**
     * Emoji used to indicate a user that their DMs are closed.
     *
     * This is only used if [the closed DMs error message][DefaultMessages.getClosedDMErrorMsg] can't be sent.
     *
     * Default: `mailbox_closed`
     *
     * Spring property: `botcommands.text.dmClosedEmoji`
     */
    @ConfigurationValue(path = "botcommands.text.dmClosedEmoji", defaultValue = "mailbox_closed", type = "java.lang.String")
    val dmClosedEmoji: Emoji
}

@ConfigDSL
class BTextConfigBuilder internal constructor() : BTextConfig {
    @set:JvmName("enable")
    override var enable: Boolean = true
    @set:JvmName("usePingAsPrefix")
    override var usePingAsPrefix: Boolean = false
    override val prefixes: MutableList<String> = mutableListOf()

    @set:JvmName("disableHelp")
    override var isHelpDisabled: Boolean = false
    @set:JvmName("showSuggestions")
    override var showSuggestions: Boolean = true

    override var dmClosedEmoji: Emoji
        get() = dmClosedEmojiSupplier()
        @Deprecated("Set dmClosedEmojiSupplier instead, retrieve with dmClosedEmoji")
        set(value) {
            dmClosedEmojiSupplier = { value }
        }
    var dmClosedEmojiSupplier: () -> Emoji = { Emoji.fromUnicode("\uD83D\uDCEA") } // mailbox_closed

    @JvmSynthetic
    internal fun build() = object : BTextConfig {
        override val enable = this@BTextConfigBuilder.enable
        override val usePingAsPrefix = this@BTextConfigBuilder.usePingAsPrefix
        override val prefixes = this@BTextConfigBuilder.prefixes.toImmutableList()
        override val isHelpDisabled = this@BTextConfigBuilder.isHelpDisabled
        override val showSuggestions = this@BTextConfigBuilder.showSuggestions
        override val dmClosedEmoji by lazy(this@BTextConfigBuilder.dmClosedEmojiSupplier)
    }
}