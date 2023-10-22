package io.github.freya022.botcommands.internal.commands.application.slash

import io.github.freya022.botcommands.api.commands.application.LengthRange
import io.github.freya022.botcommands.api.commands.application.ValueRange
import io.github.freya022.botcommands.api.commands.application.slash.builder.SlashCommandOptionAggregateBuilder
import io.github.freya022.botcommands.api.commands.application.slash.builder.SlashCommandOptionBuilder
import io.github.freya022.botcommands.api.core.utils.enumSetOf
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver
import io.github.freya022.botcommands.internal.commands.application.slash.autocomplete.AutocompleteHandler
import io.github.freya022.botcommands.internal.utils.LocalizationUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.util.*

private val logger = KotlinLogging.logger { }

class SlashCommandOption(
    slashCommandInfo: SlashCommandInfo,
    optionAggregateBuilders: Map<String, SlashCommandOptionAggregateBuilder>,
    optionBuilder: SlashCommandOptionBuilder,
    resolver: SlashParameterResolver<*, *>
) : AbstractSlashCommandOption(optionBuilder, resolver) {
    val description: String

    internal val autocompleteHandler by lazy {
        when (val autocompleteInfo = optionBuilder.autocompleteInfo) {
            null -> null
            else -> AutocompleteHandler(slashCommandInfo, optionAggregateBuilders, autocompleteInfo)
        }
    }

    val usePredefinedChoices = optionBuilder.usePredefinedChoices
    val choices: List<Command.Choice>? = optionBuilder.choices
    val range: ValueRange? = optionBuilder.valueRange
    val length: LengthRange? = optionBuilder.lengthRange

    val channelTypes: EnumSet<ChannelType> = optionBuilder.channelTypes ?: enumSetOf()

    init {
        val rootDescription = LocalizationUtils.getOptionRootDescription(slashCommandInfo.context, optionBuilder)
        description = if (optionBuilder.description.isNotBlank()) {
            // If a description was set, then use it, but check if a root description was found too
            if (rootDescription != null) {
                logger.debug { "An option description was set manually, while a root description was found in a localization bundle, path: '${slashCommandInfo.path}', option: '$declaredName'" }
            }
            optionBuilder.description
        } else {
            // If a description isn't set, then take the root description if it exists,
            // otherwise take the builder's default description
            rootDescription ?: optionBuilder.description
        }

        if (range != null) {
            if (resolver.optionType != OptionType.NUMBER && resolver.optionType != OptionType.INTEGER) {
                throw IllegalStateException("Cannot use ranges on an option that doesn't accept an integer/number")
            }
        } else if (length != null) {
            if (resolver.optionType != OptionType.STRING) {
                throw IllegalStateException("Cannot use lengths on an option that doesn't accept an string")
            }
        }
    }

    internal fun buildAutocomplete() {
        autocompleteHandler?.validateParameters()
    }

    fun hasAutocomplete() = autocompleteHandler != null
}