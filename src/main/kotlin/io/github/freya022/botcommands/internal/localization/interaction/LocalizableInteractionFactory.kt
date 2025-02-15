package io.github.freya022.botcommands.internal.localization.interaction

import io.github.freya022.botcommands.api.core.config.BLocalizationConfig
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.localization.DefaultMessagesFactory
import io.github.freya022.botcommands.api.localization.LocalizationService
import io.github.freya022.botcommands.api.localization.interaction.GuildLocaleProvider
import io.github.freya022.botcommands.api.localization.interaction.UserLocaleProvider
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

// Don't require enabled feature, could be used by user's own impl
@BService
internal class LocalizableInteractionFactory internal constructor(
    private val localizationService: LocalizationService,
    private val localizationConfig: BLocalizationConfig,
    private val userLocaleProvider: UserLocaleProvider,
    private val guildLocaleProvider: GuildLocaleProvider,
    private val defaultMessagesFactory: DefaultMessagesFactory,
) {
    internal fun create(event: IReplyCallback) =
        LocalizableInteractionImpl(event, localizationService, localizationConfig, userLocaleProvider, guildLocaleProvider, defaultMessagesFactory)
}