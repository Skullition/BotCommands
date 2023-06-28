package com.freya02.botcommands.internal.core.reflection

import com.freya02.botcommands.api.core.utils.simpleNestedName
import com.freya02.botcommands.internal.BContextImpl
import com.freya02.botcommands.internal.core.options.builder.InternalAggregators.isSingleAggregator
import com.freya02.botcommands.internal.utils.ReflectionUtils.nonInstanceParameters
import com.freya02.botcommands.internal.utils.throwInternal
import net.dv8tion.jda.api.events.Event
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class AggregatorFunction private constructor(
    boundAggregator: KFunction<*>,
    /**
     * Nullable due to constructor aggregators
     */
    private val aggregatorInstance: Any?
) : Function<Any?>(boundAggregator) {
    private val instanceParameter = aggregator.instanceParameter
    private val eventParameter = aggregator.nonInstanceParameters.firstOrNull { it.type.jvmErasure.isSubclassOf(
        Event::class) }

    val aggregator get() = this.kFunction

    val isSingleAggregator = aggregator.isSingleAggregator()

    internal constructor(context: BContextImpl, aggregator: KFunction<*>) : this(aggregator, context.serviceContainer.getFunctionServiceOrNull(aggregator))

    internal suspend fun aggregate(event: Event, aggregatorArguments: MutableMap<KParameter, Any?>): Any? {
        if (instanceParameter != null) {
            aggregatorArguments[instanceParameter] = aggregatorInstance
                ?: throwInternal(aggregator, "Aggregator's instance parameter (${instanceParameter.type.jvmErasure.simpleNestedName}) was not retrieved but was necessary")
        }

        if (eventParameter != null) {
            aggregatorArguments[eventParameter] = event
        }

        return aggregator.callSuspendBy(aggregatorArguments)
    }
}

internal fun KFunction<*>.toAggregatorFunction(context: BContextImpl) = AggregatorFunction(context, this)