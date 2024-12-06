package io.github.freya022.botcommands.api.utils;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.fellbaum.jemoji.EmojiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

/**
 * Utility class to resolve alias emojis into unicode, and getting an {@link UnicodeEmoji} out of them.
 */
public class EmojiUtils {
    private static final int REGIONAL_INDICATOR_A_CODEPOINT = 127462;
    private static final int REGIONAL_INDICATOR_Z_CODEPOINT = 127487;

    /**
     * Returns the unicode emoji from a Discord alias (e.g. {@code :joy:}).
     *
     * <p><b>Note:</b> The input string is case-sensitive!
     *
     * <p>This will return itself if the input is a valid unicode emoji.
     *
     * @param input An emoji alias or unicode
     *
     * @return The unicode string of this emoji
     *
     * @throws NoSuchElementException if no emoji alias or unicode matches
     * @see #resolveJDAEmoji(String)
     */
    @NotNull
    public static String resolveEmoji(@NotNull String input) {
        final var emoji = resolveEmojiOrNull(input);
        if (emoji == null) throw new NoSuchElementException("No emoji for input: " + input);
        return emoji;
    }

    /**
     * Returns the unicode emoji from a Discord alias (e.g. {@code :joy:}), or {@code null} if unresolvable.
     *
     * <p><b>Note:</b> The input string is case-sensitive!
     *
     * <p>This will return itself if the input is a valid unicode emoji.
     *
     * @param input An emoji alias or unicode
     *
     * @return The unicode string of this emoji, {@code null} if unresolvable
     *
     * @see #resolveJDAEmojiOrNull(String)
     */
    @Nullable
    public static String resolveEmojiOrNull(@NotNull String input) {
        var emoji = EmojiManager.getByDiscordAlias(input);

        if (emoji.isEmpty()) emoji = EmojiManager.getEmoji(input);
        if (emoji.isEmpty()) {
            // Try to get regional indicators https://github.com/felldo/JEmoji/issues/44
            final var alias = removeColonFromAlias(input);
            if (alias.startsWith("regional_indicator_")) {
                final char character = alias.charAt(19);
                if (character >= 'a' && character <= 'z') {
                    final int codepoint = REGIONAL_INDICATOR_A_CODEPOINT + (character - 'a');
                    return Character.toString(codepoint);
                }
            } else {
                final int codepoint = input.codePointAt(0);
                if (codepoint >= REGIONAL_INDICATOR_A_CODEPOINT && codepoint <= REGIONAL_INDICATOR_Z_CODEPOINT) {
                    return input;
                }
            }
            return null;
        }
        return emoji.get().getUnicode();
    }

    @NotNull
    private static String removeColonFromAlias(@NotNull final String alias) {
        return alias.startsWith(":") && alias.endsWith(":") ? alias.substring(1, alias.length() - 1) : alias;
    }

    /**
     * Returns the {@link UnicodeEmoji} from a Discord alias (e.g. {@code :joy:}).
     *
     * <p><b>Note:</b> The input string is case-sensitive!
     *
     * <p>This will return itself if the input is a valid unicode emoji.
     *
     * @param input An emoji alias or unicode
     *
     * @return The {@link UnicodeEmoji} of this emoji
     *
     * @throws NoSuchElementException if no emoji alias or unicode matches
     * @see #resolveEmoji(String)
     */
    @NotNull
    public static UnicodeEmoji resolveJDAEmoji(@NotNull String input) {
        return Emoji.fromUnicode(resolveEmoji(input));
    }

    /**
     * Returns the {@link UnicodeEmoji} from a Discord alias (e.g. {@code :joy:}), or {@code null} if unresolvable.
     *
     * <p><b>Note:</b> The input string is case-sensitive!
     *
     * <p>This will return itself if the input is a valid unicode emoji.
     *
     * @param input An emoji alias or unicode
     *
     * @return The {@link UnicodeEmoji} of this emoji
     *
     * @see #resolveEmoji(String)
     */
    @Nullable
    public static UnicodeEmoji resolveJDAEmojiOrNull(@NotNull String input) {
        final String unicode = resolveEmojiOrNull(input);
        if (unicode == null) return null;
        return Emoji.fromUnicode(unicode);
    }

    /**
     * Converts the provided {@link net.fellbaum.jemoji.Emoji Emoji} into a JDA {@link UnicodeEmoji}.
     *
     * <p>I highly recommend putting your emojis in a class that is loaded only when necessary,
     * avoiding any unnecessary startup delay.
     * You can do so by using a static inner class, for example:
     *
     * <pre><code>
     * static class Emojis {
     *     private static final UnicodeEmoji WASTEBASKET = EmojiUtils.asUnicodeEmoji(net.fellbaum.jemoji.Emojis.WASTEBASKET);
     * }
     * </code></pre>
     *
     * @param emoji
     * @return
     */
    @NotNull
    public static UnicodeEmoji asUnicodeEmoji(@NotNull net.fellbaum.jemoji.Emoji emoji) {
        return Emoji.fromUnicode(emoji.getEmoji());
    }
}
