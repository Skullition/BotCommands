package io.github.freya022.botcommands.internal.localization.interaction

import io.github.freya022.botcommands.api.localization.Localization
import io.github.freya022.botcommands.api.localization.interaction.LocalizableEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction
import java.util.*

internal class LocalizableEditCallbackImpl internal constructor(
    private val editCallback: IMessageEditCallback,
    private val interaction: LocalizableInteractionImpl
) : LocalizableEditCallback {

    override fun editUser(localizationPath: String, vararg entries: Localization.Entry): MessageEditCallbackAction =
        editCallback.editMessage(interaction.getUserMessage(localizationPath, *entries))

    override fun editGuild(localizationPath: String, vararg entries: Localization.Entry): MessageEditCallbackAction =
        editCallback.editMessage(interaction.getGuildMessage(localizationPath, *entries))

    override fun editLocalized(
        locale: Locale,
        localizationPath: String,
        vararg entries: Localization.Entry,
    ): MessageEditCallbackAction {
        return editCallback.editMessage(interaction.getLocalizedMessage(locale, localizationPath, *entries))
    }
}