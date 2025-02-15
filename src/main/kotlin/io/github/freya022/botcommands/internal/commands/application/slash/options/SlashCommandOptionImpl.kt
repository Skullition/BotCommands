package io.github.freya022.botcommands.internal.commands.application.slash.options

import io.github.freya022.botcommands.api.commands.application.LengthRange
import io.github.freya022.botcommands.api.commands.application.ValueRange
import io.github.freya022.botcommands.api.core.config.BApplicationConfigBuilder
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver
import io.github.freya022.botcommands.internal.commands.application.options.ApplicationCommandOptionImpl
import io.github.freya022.botcommands.internal.commands.application.slash.autocomplete.AutocompleteHandler
import io.github.freya022.botcommands.internal.commands.application.slash.options.builder.SlashCommandOptionBuilderImpl
import io.github.freya022.botcommands.internal.utils.LocalizationUtils
import io.github.freya022.botcommands.internal.utils.classRef
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType

internal class SlashCommandOptionImpl internal constructor(
    override val parent: SlashCommandParameterImpl,
    optionBuilder: SlashCommandOptionBuilderImpl,
    override val resolver: SlashParameterResolver<*, *>
) : ApplicationCommandOptionImpl(optionBuilder),
    SlashCommandOptionMixin {

    override val executable get() = parent.executable

    override val description: String

    internal val autocompleteHandler by lazy {
        when (val autocompleteInfo = optionBuilder.autocompleteInfo) {
            null -> null
            else -> AutocompleteHandler(parent.executable, autocompleteInfo)
        }
    }

    override val discordName = optionBuilder.optionName
    override val usePredefinedChoices = optionBuilder.usePredefinedChoices
    override val choices: List<Command.Choice>? = optionBuilder.choices
    override val range: ValueRange? = optionBuilder.valueRange
    override val length: LengthRange? = optionBuilder.lengthRange

    init {
        choices?.forEach {
            check(it.nameLocalizations.toMap().isEmpty()) {
                "Choice '${it.name}' cannot be manually localized, use ${classRef<BApplicationConfigBuilder>()}#addLocalizations instead"
            }
        }

        description = LocalizationUtils.getOptionDescription(executable.context, optionBuilder)

        if (range != null) {
            check(resolver.optionType == OptionType.NUMBER || resolver.optionType == OptionType.INTEGER) {
                "Cannot use ranges on an option that doesn't accept an integer/number"
            }
        } else if (length != null) {
            check(resolver.optionType == OptionType.STRING) {
                "Cannot use lengths on an option that doesn't accept an string"
            }
        }
    }

    internal fun buildAutocomplete() {
        autocompleteHandler?.validateParameters()
    }

    override fun hasAutocomplete() = autocompleteHandler != null

    override fun invalidateAutocomplete() {
        check(hasAutocomplete()) {
            "There is no autocomplete on this option"
        }
        autocompleteHandler!!.invalidate()
    }
}