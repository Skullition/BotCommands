package io.github.freya022.botcommands.api.parameters.resolvers

import io.github.freya022.botcommands.api.commands.text.BaseCommandEvent
import io.github.freya022.botcommands.api.commands.text.TextCommandVariation
import io.github.freya022.botcommands.api.commands.text.annotations.JDATextCommandVariation
import io.github.freya022.botcommands.api.commands.text.options.TextCommandOption
import io.github.freya022.botcommands.api.parameters.ParameterResolver
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.regex.Pattern
import kotlin.reflect.KParameter
import kotlin.reflect.KType

/**
 * Parameter resolver for parameters of [@JDATextCommandVariation][JDATextCommandVariation].
 *
 * Needs to be implemented alongside a [ParameterResolver] subclass.
 *
 * @param T Type of the implementation
 * @param R Type of the returned resolved objects
 */
@Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")
interface TextParameterResolver<T, R : Any> : IParameterResolver<T>
        where T : ParameterResolver<T, R>,
              T : TextParameterResolver<T, R> {

    /**
     * Returns a resolved object from this text command.
     *
     * If this returns `null`, and the parameter is required, i.e., not [nullable][KType.isMarkedNullable]
     * or [optional][KParameter.isOptional], then the handler goes to the next command variation.
     *
     * See the [@JDATextCommandVariation][JDATextCommandVariation] documentation for more details about text command variations.
     *
     * @param option The option currently being resolved
     * @param event  The corresponding event
     * @param args   The arguments of this parameter, extracted with [pattern]
     */
    fun resolve(option: TextCommandOption, event: MessageReceivedEvent, args: Array<String?>): R? =
        resolve(option.executable, event, args)

    @Deprecated("Replaced TextCommandVariation with TextCommandOption")
    fun resolve(variation: TextCommandVariation, event: MessageReceivedEvent, args: Array<String?>): R? =
        throw NotImplementedError("${this.javaClass.simpleName} must implement the 'resolve' or 'resolveSuspend' method")

    /**
     * Returns a resolved object from this text command.
     *
     * If this returns `null`, and the parameter is required, i.e., not [nullable][KType.isMarkedNullable]
     * or [optional][KParameter.isOptional], then the handler goes to the next command variation.
     *
     * See the [@JDATextCommandVariation][JDATextCommandVariation] documentation for more details about text command variations.
     *
     * @param option The option currently being resolved
     * @param event  The corresponding event
     * @param args   The arguments of this parameter, extracted with [pattern]
     */
    @JvmSynthetic
    suspend fun resolveSuspend(option: TextCommandOption, event: MessageReceivedEvent, args: Array<String?>) =
        resolve(option, event, args)

    @Deprecated("Replaced TextCommandVariation with TextCommandOption")
    @JvmSynthetic
    suspend fun resolveSuspend(variation: TextCommandVariation, event: MessageReceivedEvent, args: Array<String?>) =
        resolve(variation, event, args)

    /**
     * The minimum group count to be extracted from the match result.
     *
     * This may be overridden if your pattern provides more groups than it actually needs to match a string,
     * such as patterns with `|`.
     */
    @Deprecated("No longer required")
    val requiredGroups: Int get() = preferredPattern.matcher("").groupCount()

    /**
     * Returns the pattern required to recognize this parameter,
     * used in a larger pattern representing the entire command.
     *
     * ### Pattern flags
     *
     * Flags added on this pattern will not affect other options,
     * however, make sure to close "embedded flag expressions" if you do use them.
     *
     * Using flags without embedded flag expressions is not allowed.
     *
     * [Pattern.UNICODE_CASE] and [Pattern.UNICODE_CHARACTER_CLASS] are enabled by default.
     *
     * @return The [Pattern] for this parameter
     */
    val pattern: Pattern

    /**
     * Returns an example string for this parameter
     *
     * This is only used to construct an example command internally and test the whole command's regex against it
     *
     * If the regex does not match the constructed example command, the framework will throw as the regex is deemed "too complex"
     *
     * @return An example string for validation purposes
     */
    val testExample: String

    val preferredPattern: Pattern
        get() = pattern

    /**
     * Returns a help example for the supplied option.
     *
     * **Tip:** You may use the event as a way to get sample data (such as getting the member, channel, roles, etc...).
     *
     * @param option    The option of the command being shown in the help content
     * @param event     The event of the command that triggered help content to be displayed
     */
    fun getHelpExample(option: TextCommandOption, event: BaseCommandEvent): String
}