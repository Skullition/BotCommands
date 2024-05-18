package io.github.freya022.botcommands.api.modals

import io.github.freya022.botcommands.api.modals.annotations.ModalData
import io.github.freya022.botcommands.api.modals.annotations.ModalHandler
import io.github.freya022.botcommands.internal.modals.ModalDSL
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import javax.annotation.CheckReturnValue
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import net.dv8tion.jda.api.interactions.modals.Modal as JDAModal
import java.time.Duration as JavaDuration

@ModalDSL
abstract class ModalBuilder protected constructor(
    customId: String,
    title: String
) : JDAModal.Builder(customId, title) {
    /**
     * Binds the action to a [@ModalHandler][ModalHandler] with its arguments.
     *
     * @param handlerName The name of the modal handler, which must be the same as your [@ModalHandler][ModalHandler]
     * @param userData    The optional user data to be passed to the modal handler via [@ModalData][ModalData]
     *
     * @return This builder for chaining convenience
     */
    @CheckReturnValue
    abstract fun bindTo(handlerName: String, userData: List<Any?>): ModalBuilder

    /**
     * Binds the action to a [@ModalHandler][ModalHandler] with its arguments.
     *
     * @param handlerName The name of the modal handler, which must be the same as your [@ModalHandler][ModalHandler]
     * @param userData    The optional user data to be passed to the modal handler via [@ModalData][ModalData]
     *
     * @return This builder for chaining convenience
     */
    @CheckReturnValue
    fun bindTo(handlerName: String, vararg userData: Any?): ModalBuilder {
        return bindTo(handlerName, userData.asList())
    }

    /**
     * Binds the action to the consumer.
     *
     * @param handler The modal handler to run when the modal is used
     *
     * @return This builder for chaining convenience
     */
    @CheckReturnValue
    fun bindTo(handler: Consumer<ModalEvent>): ModalBuilder {
        return bindTo { handler.accept(it) }
    }

    /**
     * Binds the action to the closure.
     *
     * @param handler The modal handler to run when the modal is used
     *
     * @return This builder for chaining convenience
     */
    @JvmSynthetic
    abstract fun bindTo(handler: suspend (ModalEvent) -> Unit): ModalBuilder

    /**
     * Sets the timeout for this modal, invalidating the modal after expiration,
     * and running the given timeout handler.
     *
     * If unset, the timeout is set to [Modals.defaultTimeout].
     *
     * @param timeout   The amount of time in the supplied time unit before the modal is removed
     * @param unit      The time unit of the timeout
     * @param onTimeout The function to run when the timeout has been reached
     *
     * @return This builder for chaining convenience
     */
    @JvmOverloads
    @CheckReturnValue
    fun timeout(timeout: Long, unit: TimeUnit, onTimeout: Runnable? = null): ModalBuilder {
        return timeout(JavaDuration.of(timeout, unit.toChronoUnit()), onTimeout)
    }

    /**
     * Sets the timeout for this modal, invalidating the modal after expiration,
     * and running the given timeout handler.
     *
     * If unset, the timeout is set to [Modals.defaultTimeout].
     *
     * @param timeout   The amount of time before the modal is removed
     * @param onTimeout The function to run when the timeout has been reached
     *
     * @return This builder for chaining convenience
     */
    @JvmOverloads
    @CheckReturnValue
    fun timeout(timeout: JavaDuration, onTimeout: Runnable? = null): ModalBuilder {
        return timeout(timeout.toKotlinDuration(), onTimeout?.let { { onTimeout.run() } })
    }

    /**
     * Sets the timeout for this modal, invalidating the modal after expiration,
     * and running the given timeout handler.
     *
     * If unset, the timeout is set to [Modals.defaultTimeout].
     *
     * @param timeout   The amount of time before the modal is removed
     * @param onTimeout The function to run when the timeout has been reached
     *
     * @return This builder for chaining convenience
     */
    @JvmSynthetic
    abstract fun timeout(timeout: Duration, onTimeout: (suspend () -> Unit)? = null): ModalBuilder

    @Deprecated("Cannot set an ID on modals managed by the framework", level = DeprecationLevel.ERROR)
    abstract override fun setId(customId: String): ModalBuilder

    protected fun internetSetId(customId: String) {
        super.setId(customId)
    }

    protected fun jdaBuild(): JDAModal {
        return super.build()
    }

    @CheckReturnValue
    abstract override fun build(): Modal
}
