@file:IgnoreStackFrame // Due to extensions

package io.github.freya022.botcommands.api.commands.ratelimit.declaration

import io.github.freya022.botcommands.api.ReceiverConsumer
import io.github.freya022.botcommands.api.commands.annotations.Cooldown
import io.github.freya022.botcommands.api.commands.annotations.RateLimitReference
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.declaration.AutocompleteHandlerProvider
import io.github.freya022.botcommands.api.commands.builder.CommandBuilder
import io.github.freya022.botcommands.api.commands.builder.RateLimitBuilder
import io.github.freya022.botcommands.api.commands.ratelimit.CancellableRateLimit
import io.github.freya022.botcommands.api.commands.ratelimit.RateLimitInfo
import io.github.freya022.botcommands.api.commands.ratelimit.RateLimitScope
import io.github.freya022.botcommands.api.commands.ratelimit.RateLimiter
import io.github.freya022.botcommands.api.commands.ratelimit.bucket.Buckets
import io.github.freya022.botcommands.api.commands.ratelimit.bucket.toSupplier
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.BotOwners
import io.github.freya022.botcommands.api.core.annotations.IgnoreStackFrame
import kotlin.time.Duration
import java.time.Duration as JavaDuration

/**
 * Allows programmatic declaration of autocomplete handlers using [AutocompleteHandlerProvider].
 *
 * @see AutocompleteHandlerProvider
 */
@IgnoreStackFrame // Due to the abstract method
abstract class RateLimitManager internal constructor() {
    abstract val context: BContext

    protected abstract fun createRateLimit(
        group: String,
        rateLimiter: RateLimiter,
        block: RateLimitBuilder.() -> Unit
    ): RateLimitInfo

    /**
     * Creates a rate limiter with the specified group.
     *
     * The created rate limiter can be used in [CommandBuilder.rateLimitReference] and [@RateLimitReference][RateLimitReference].
     *
     * **Note:** The rate limiter won't apply if you are a [bot owner][BotOwners.isOwner].
     *
     * @param group       The name of the rate limiter
     * @param rateLimiter The [RateLimiter] in charge of retrieving buckets and handling rate limits
     * @param block       Further configures the [RateLimitBuilder]
     *
     * @throws IllegalStateException If a rate limiter with the same group exists
     */
    @JvmOverloads
    fun rateLimit(
        group: String,
        rateLimiter: RateLimiter,
        block: ReceiverConsumer<RateLimitBuilder> = ReceiverConsumer.noop()
    ): RateLimitInfo {
        return createRateLimit(group, rateLimiter, block)
    }

    /**
     * Creates a rate limit-based cooldown.
     *
     * As this is a convenience method, any cooldown applied will be lost on restart.
     * To persist the cooldowns, use [rateLimit] with [RateLimiter.createDefaultProxied] and a [Buckets.ofCooldown].
     *
     * **Note:** The cooldown won't apply if you are a [bot owner][BotOwners.isOwner].
     *
     * ### Cooldown cancellation
     * The cooldown can be cancelled inside the command with [CancellableRateLimit.cancelRateLimit] on your event.
     *
     * @param group    The name of the underlying rate limiter
     * @param scope    The scope of the cooldown
     * @param duration The duration before the cooldown expires
     * @param block    Further configures the [RateLimitBuilder]
     *
     * @see Cooldown @Cooldown
     * @see rateLimit
     */
    @JvmOverloads
    fun cooldown(
        group: String,
        duration: JavaDuration,
        scope: RateLimitScope = RateLimitScope.USER,
        deleteOnRefill: Boolean = true,
        block: ReceiverConsumer<RateLimitBuilder> = ReceiverConsumer.noop()
    ): RateLimitInfo {
        return rateLimit(group, RateLimiter.createDefault(scope, Buckets.ofCooldown(duration).toSupplier(), deleteOnRefill), block)
    }
}

/**
 * Creates a rate limit-based cooldown.
 *
 * As this is a convenience method, any cooldown applied will be lost on restart.
 * To persist the cooldowns, use [rateLimit][RateLimitManager.rateLimit] with [RateLimiter.createDefaultProxied] and a [Buckets.ofCooldown].
 *
 * **Note:** The cooldown won't apply if you are a [bot owner][BotOwners.isOwner].
 *
 * ### Cooldown cancellation
 * The cooldown can be cancelled inside the command with [CancellableRateLimit.cancelRateLimit] on your event.
 *
 * @param group          The name of the underlying rate limiter
 * @param scope          The scope of the cooldown
 * @param duration       The duration before the cooldown expires
 * @param deleteOnRefill Whether the cooldown message should be deleted after the cooldown expires
 * @param block          Further configures the [RateLimitBuilder]
 *
 * @see Cooldown @Cooldown
 * @see RateLimitManager.rateLimit
 */
fun RateLimitManager.cooldown(
    group: String,
    duration: Duration,
    scope: RateLimitScope = RateLimitScope.USER,
    deleteOnRefill: Boolean = true,
    block: ReceiverConsumer<RateLimitBuilder> = ReceiverConsumer.noop()
): RateLimitInfo {
    return rateLimit(group, RateLimiter.createDefault(scope, Buckets.ofCooldown(duration).toSupplier(), deleteOnRefill), block)
}