package io.github.freya022.botcommands.api.core.utils

import io.github.freya022.botcommands.api.utils.EmojiUtils
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji

/**
 * Lazily fetches an [UnicodeEmoji] from the provided emoji alias, Unicode characters, or Markdown.
 *
 * ### Example
 * ```kt
 * // Emoji alias
 * private val smiley by lazyJDAEmoji(":smiley:")
 * // Unicode emoji
 * private val smiley by lazyJDAEmoji("😃")
 * ```
 */
fun lazyJDAEmoji(input: String): Lazy<UnicodeEmoji> =
    lazy { EmojiUtils.resolveJDAEmoji(input) }

/**
 * Lazily fetches the Unicode from the provided emoji alias.
 *
 * If the input is already a Unicode emoji, it will be returned.
 *
 * ### Example
 * ```kt
 * // Emoji alias
 * private val smiley by lazyEmoji("smiley")
 * // Unicode emoji
 * private val smiley by lazyEmoji("😃")
 * ```
 */
fun lazyEmoji(input: String): Lazy<String> =
    lazy { EmojiUtils.resolveEmoji(input) }