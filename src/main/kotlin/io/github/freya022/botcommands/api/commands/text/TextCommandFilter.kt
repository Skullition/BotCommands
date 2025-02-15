package io.github.freya022.botcommands.api.commands.text

import io.github.freya022.botcommands.api.commands.text.builder.TextCommandBuilder
import io.github.freya022.botcommands.api.core.Filter
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.service.annotations.InterfacedService
import io.github.freya022.botcommands.api.core.utils.simpleNestedName
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 * Prevents text command execution by returning an error object to the command executor.
 *
 * Filters run when a [command variation][TextCommandBuilder.variation] is about to be executed,
 * i.e., after the permissions/rate limits... were checked.
 *
 * When the final filter returns an error object of type [T],
 * it will then be passed to the [TextCommandRejectionHandler].
 *
 * ### Combining filters
 *
 * Filters can be combined with [`and`][and]/[`or`][or] (static methods for Java users).
 *
 * ### Requirements
 * - Register your instance as a service with [@BService][BService].
 * This is not required if you pass the instance directly to the command builder.
 * - Have exactly one instance of [TextCommandRejectionHandler].
 * - Implement either [check] (Java) or [checkSuspend] (Kotlin).
 * - (Optional) Set your filter as a command-specific filter by disabling [global].
 *
 * ### Execution order
 * The execution order of global filters is determined by the priority of the service,
 * while command-specific filters use the insertion order.
 *
 * ### Example - Accepting commands only in a single channel
 * ```kt
 * @BService
 * class MyTextCommandFilter : TextCommandFilter<String> {
 *     override suspend fun checkSuspend(event: MessageReceivedEvent, commandVariation: TextCommandVariation, args: String): String? {
 *         if (event.guildChannel.idLong != 722891685755093076) {
 *             return "Can only run commands in <#722891685755093076>"
 *         }
 *         return null
 *     }
 * }
 * ```
 *
 * <Hr>
 *
 * ```java
 * @BService
 * public class MyTextCommandFilter implements TextCommandFilter<String> {
 *     @Nullable
 *     @Override
 *     public String check(@NotNull MessageReceivedEvent event, @NotNull TextCommandVariation commandVariation, @NotNull String args) {
 *         if (channel.getIdLong() != 722891685755093076L) {
 *             return "Can only run commands in <#722891685755093076>";
 *         }
 *         return null;
 *     }
 * }
 * ```
 *
 * @param T Type of the error object handled by [TextCommandRejectionHandler]
 *
 * @see TextCommandRejectionHandler
 * @see InterfacedService @InterfacedService
 */
@InterfacedService(acceptMultiple = true)
interface TextCommandFilter<T : Any> : Filter {
    /**
     * Returns `null` if this filter should allow the command to run, or returns your own object if it can't.
     *
     * The object will be passed to your [TextCommandRejectionHandler] if the command is rejected.
     */
    @JvmSynthetic
    suspend fun checkSuspend(event: MessageReceivedEvent, commandVariation: TextCommandVariation, args: String): T? =
        check(event, commandVariation, args)

    /**
     * Returns `null` if this filter should allow the command to run, or returns your own object if it can't.
     *
     * The object will be passed to your [TextCommandRejectionHandler] if the command is rejected.
     */
    fun check(event: MessageReceivedEvent, commandVariation: TextCommandVariation, args: String): T? =
        throw NotImplementedError("${this.javaClass.simpleNestedName} must implement the 'check' or 'checkSuspend' method")
}

infix fun <T : Any> TextCommandFilter<T>.or(other: TextCommandFilter<T>): TextCommandFilter<T> {
    return object : TextCommandFilter<T> {
        override val global: Boolean = false

        override val description: String
            get() = "(${this@or.description} || ${other.description})"

        override suspend fun checkSuspend(
            event: MessageReceivedEvent,
            commandVariation: TextCommandVariation,
            args: String
        ): T? {
            // Elvis operator short circuits if left condition had no error
            this@or.checkSuspend(event, commandVariation, args) ?: return null
            return other.checkSuspend(event, commandVariation, args)
        }
    }
}

infix fun <T : Any> TextCommandFilter<T>.and(other: TextCommandFilter<T>): TextCommandFilter<T> {
    return object : TextCommandFilter<T> {
        override val global: Boolean = false

        override val description: String
            get() = "(${this@and.description} && ${other.description})"

        override suspend fun checkSuspend(event: MessageReceivedEvent, commandVariation: TextCommandVariation, args: String): T? {
            val errorObject = this@and.checkSuspend(event, commandVariation, args)
            if (errorObject != null)
                return errorObject
            return other.checkSuspend(event, commandVariation, args)
        }
    }
}