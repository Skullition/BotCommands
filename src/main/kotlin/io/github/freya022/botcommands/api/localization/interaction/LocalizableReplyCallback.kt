package io.github.freya022.botcommands.api.localization.interaction

import io.github.freya022.botcommands.api.core.config.BLocalizationConfig
import io.github.freya022.botcommands.api.localization.Localization
import io.github.freya022.botcommands.api.localization.context.PairEntry
import io.github.freya022.botcommands.api.localization.context.mapToEntries
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import java.util.*
import javax.annotation.CheckReturnValue

/**
 * Allows replying to interactions using localized strings,
 * registered from bundles in [BLocalizationConfig.responseBundles].
 *
 * See [LocalizableInteraction] for further configuration.
 *
 * @see BLocalizationConfig.responseBundles
 */
interface LocalizableReplyCallback {
    /**
     * Replies with the localized message at the following [path][localizationPath],
     * using the user's locale and provided parameters.
     *
     * ### Bundle resolution
     * The bundle used is either the [defined bundle][LocalizableInteraction.localizationBundle]
     * or one of the [registered bundles][BLocalizationConfig.responseBundles].
     *
     * The locale of the bundle is the best available,
     * for example, if `fr_FR` is not available, then `fr` will be used,
     * and otherwise, the root bundle (without any suffix) will be used.
     *
     * @param localizationPath The path of the message to translate,
     * will be prefixed with [localizationPrefix][LocalizableInteraction.localizationPrefix]
     * @param entries          The values replacing arguments of the localization template
     *
     * @throws IllegalArgumentException If:
     * - [localizationBundle][LocalizableInteraction.localizationBundle] is set, but the bundle doesn't exist
     * - No [registered bundle][BLocalizationConfig.responseBundles] containing the path could be found
     * - If the template requires an argument that was not passed to [entries]
     */
    @CheckReturnValue
    fun replyUser(localizationPath: String, vararg entries: Localization.Entry): ReplyCallbackAction

    /**
     * Replies with the localized message at the following [path][localizationPath],
     * using the guild's locale and provided parameters.
     *
     * ### Bundle resolution
     * The bundle used is either the [defined bundle][LocalizableInteraction.localizationBundle]
     * or one of the [registered bundles][BLocalizationConfig.responseBundles].
     *
     * The locale of the bundle is the best available,
     * for example, if `fr_FR` is not available, then `fr` will be used,
     * and otherwise, the root bundle (without any suffix) will be used.
     *
     * @param localizationPath The path of the message to translate,
     * will be prefixed with [localizationPrefix][LocalizableInteraction.localizationPrefix]
     * @param entries          The values replacing arguments of the localization template
     *
     * @throws IllegalArgumentException If:
     * - [localizationBundle][LocalizableInteraction.localizationBundle] is set, but the bundle doesn't exist
     * - No [registered bundle][BLocalizationConfig.responseBundles] containing the path could be found
     * - If the template requires an argument that was not passed to [entries]
     */
    @CheckReturnValue
    fun replyGuild(localizationPath: String, vararg entries: Localization.Entry): ReplyCallbackAction

    /**
     * Replies with the localized message at the following [path][localizationPath],
     * using the provided locale and parameters.
     *
     * ### Bundle resolution
     * The bundle used is either the [defined bundle][LocalizableInteraction.localizationBundle]
     * or one of the [registered bundles][BLocalizationConfig.responseBundles].
     *
     * The locale of the bundle is the best available,
     * for example, if `fr_FR` is not available, then `fr` will be used,
     * and otherwise, the root bundle (without any suffix) will be used.
     *
     * @param localizationPath The path of the message to translate,
     * will be prefixed with [localizationPrefix][LocalizableInteraction.localizationPrefix]
     * @param entries          The values replacing arguments of the localization template
     *
     * @throws IllegalArgumentException If:
     * - [localizationBundle][LocalizableInteraction.localizationBundle] is set, but the bundle doesn't exist
     * - No [registered bundle][BLocalizationConfig.responseBundles] containing the path could be found
     * - If the template requires an argument that was not passed to [entries]
     */
    @CheckReturnValue
    fun replyLocalized(locale: DiscordLocale, localizationPath: String, vararg entries: Localization.Entry): ReplyCallbackAction =
        replyLocalized(locale.toLocale(), localizationPath, *entries)

    /**
     * Replies with the localized message at the following [path][localizationPath],
     * using the provided locale and parameters.
     *
     * ### Bundle resolution
     * The bundle used is either the [defined bundle][LocalizableInteraction.localizationBundle]
     * or one of the [registered bundles][BLocalizationConfig.responseBundles].
     *
     * The locale of the bundle is the best available,
     * for example, if `fr_FR` is not available, then `fr` will be used,
     * and otherwise, the root bundle (without any suffix) will be used.
     *
     * @param localizationPath The path of the message to translate,
     * will be prefixed with [localizationPrefix][LocalizableInteraction.localizationPrefix]
     * @param entries          The values replacing arguments of the localization template
     *
     * @throws IllegalArgumentException If:
     * - [localizationBundle][LocalizableInteraction.localizationBundle] is set, but the bundle doesn't exist
     * - No [registered bundle][BLocalizationConfig.responseBundles] containing the path could be found
     * - If the template requires an argument that was not passed to [entries]
     */
    @CheckReturnValue
    fun replyLocalized(locale: Locale, localizationPath: String, vararg entries: Localization.Entry): ReplyCallbackAction
}

fun LocalizableReplyCallback.replyUser(localizationPath: String, vararg entries: PairEntry) =
    replyUser(localizationPath, *entries.mapToEntries())

fun LocalizableReplyCallback.replyGuild(localizationPath: String, vararg entries: PairEntry) =
    replyGuild(localizationPath, *entries.mapToEntries())

fun LocalizableReplyCallback.replyLocalized(locale: DiscordLocale, localizationPath: String, vararg entries: PairEntry) =
    replyLocalized(locale, localizationPath, *entries.mapToEntries())

fun LocalizableReplyCallback.replyLocalized(locale: Locale, localizationPath: String, vararg entries: PairEntry) =
    replyLocalized(locale, localizationPath, *entries.mapToEntries())