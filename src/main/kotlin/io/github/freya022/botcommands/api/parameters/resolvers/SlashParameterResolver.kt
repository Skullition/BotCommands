package io.github.freya022.botcommands.api.parameters.resolvers

import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.SlashCommandInfo
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption
import io.github.freya022.botcommands.api.commands.application.slash.options.builder.SlashCommandOptionBuilder
import io.github.freya022.botcommands.api.localization.DefaultMessages
import io.github.freya022.botcommands.api.parameters.ParameterResolver
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.reflect.KParameter
import kotlin.reflect.KType

/**
 * Parameter resolver for parameters of [@JDASlashCommand][JDASlashCommand] and [@AutocompleteHandler][AutocompleteHandler].
 *
 * Needs to be implemented alongside a [ParameterResolver] subclass.
 *
 * @param T Type of the implementation
 * @param R Type of the returned resolved objects
 */
@Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")
interface SlashParameterResolver<T, R : Any> : IParameterResolver<T>
        where T : ParameterResolver<T, R>,
              T : SlashParameterResolver<T, R> {

    /**
     * Returns the corresponding [OptionType] for this slash command parameter.
     */
    val optionType: OptionType

    /**
     * Returns a constant list of [choices][Choice] for this slash parameter resolver.
     *
     * This will be applied to all command parameters of this type,
     * but can still be overridden if there are choices set in [ApplicationCommand.getOptionChoices].
     *
     * This could be useful for, say, an enum resolver, or anything where the choices do not change between commands.
     *
     * **Note:** This requires enabling [SlashOption.usePredefinedChoices] (annotation-declared) / [SlashCommandOptionBuilder.usePredefinedChoices] (DSL-declared).
     */
    fun getPredefinedChoices(guild: Guild?): Collection<Choice> {
        return emptyList()
    }

    /**
     * Returns a resolved object for this [OptionMapping].
     *
     * If this returns `null`, and the parameter is required, i.e., not [nullable][KType.isMarkedNullable]
     * or [optional][KParameter.isOptional], then the interaction is ignored,
     * and you should reply if this is a [SlashCommandInteractionEvent].
     *
     * If the interaction is not replied to,
     * the handler sends an [unresolvable option error message][DefaultMessages.getSlashCommandUnresolvableOptionMsg].
     *
     * @param option        The option currently being resolved
     * @param event         The corresponding event, could be a [SlashCommandInteractionEvent] or a [CommandAutoCompleteInteractionEvent]
     * @param optionMapping The [OptionMapping] to be resolved
     */
    fun resolve(option: SlashCommandOption, event: CommandInteractionPayload, optionMapping: OptionMapping): R? =
        resolve(option.executable, event, optionMapping)

    @Deprecated("Replaced SlashCommandInfo with SlashCommandOption")
    fun resolve(info: SlashCommandInfo, event: CommandInteractionPayload, optionMapping: OptionMapping): R? =
        throw NotImplementedError("${this.javaClass.simpleName} must implement the 'resolve' or 'resolveSuspend' method")

    /**
     * Returns a resolved object for this [OptionMapping].
     *
     * If this returns `null`, and the parameter is required, i.e., not [nullable][KType.isMarkedNullable]
     * or [optional][KParameter.isOptional], then the interaction is ignored,
     * and you should reply if this is a [SlashCommandInteractionEvent].
     *
     * If the interaction is not replied to,
     * the handler sends an [unresolvable option error message][DefaultMessages.getSlashCommandUnresolvableOptionMsg].
     *
     * @param option        The option currently being resolved
     * @param event         The corresponding event, could be a [SlashCommandInteractionEvent] or a [CommandAutoCompleteInteractionEvent]
     * @param optionMapping The [OptionMapping] to be resolved
     */
    @JvmSynthetic
    suspend fun resolveSuspend(option: SlashCommandOption, event: CommandInteractionPayload, optionMapping: OptionMapping) =
        resolve(option, event, optionMapping)

    @JvmSynthetic
    @Deprecated("Replaced SlashCommandInfo with SlashCommandOption")
    suspend fun resolveSuspend(info: SlashCommandInfo, event: CommandInteractionPayload, optionMapping: OptionMapping) =
        resolve(info, event, optionMapping)
}
