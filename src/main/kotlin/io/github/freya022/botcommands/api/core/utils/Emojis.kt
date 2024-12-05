package io.github.freya022.botcommands.api.core.utils

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji
import net.fellbaum.jemoji.Emoji as JEmoji

/**
 * Converts this JEmoji [Emoji][JEmoji] into a JDA [UnicodeEmoji].
 *
 * @see lazyUnicodeEmoji
 */
fun JEmoji.asUnicodeEmoji(): UnicodeEmoji = Emoji.fromUnicode(emoji)

/**
 * Lazily converts the supplied [Emoji][JEmoji] into a JDA [UnicodeEmoji].
 *
 * This is useful to load JEmoji only when it's necessary, avoiding any startup delay.
 */
fun lazyUnicodeEmoji(supplier: () -> JEmoji): Lazy<UnicodeEmoji> =
    lazy { supplier().asUnicodeEmoji() }