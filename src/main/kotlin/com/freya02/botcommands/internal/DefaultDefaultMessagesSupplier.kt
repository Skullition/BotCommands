package com.freya02.botcommands.internal

import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.DefaultMessages
import com.freya02.botcommands.api.core.DefaultMessagesSupplier
import net.dv8tion.jda.api.interactions.DiscordLocale
import java.util.*

//@BService //TODO user-defined implementation detection
class DefaultDefaultMessagesSupplier(private val context: BContext) : DefaultMessagesSupplier {
    private val localeDefaultMessagesMap: MutableMap<DiscordLocale, DefaultMessages> = EnumMap(DiscordLocale::class.java)

    override fun get(discordLocale: DiscordLocale): DefaultMessages {
        return localeDefaultMessagesMap.computeIfAbsent(discordLocale) { DefaultMessages(context, Locale.forLanguageTag(it.locale)) }
    }
}