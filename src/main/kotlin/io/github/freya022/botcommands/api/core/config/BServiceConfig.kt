package io.github.freya022.botcommands.api.core.config

import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.core.annotations.Handler
import io.github.freya022.botcommands.api.core.service.InstanceSupplier
import io.github.freya022.botcommands.api.core.service.annotations.*
import io.github.freya022.botcommands.api.core.utils.toImmutableMap
import io.github.freya022.botcommands.api.core.utils.toImmutableSet
import io.github.freya022.botcommands.api.core.utils.unmodifiableView
import io.github.freya022.botcommands.internal.core.config.ConfigDSL
import kotlin.reflect.KClass

@InjectedService
interface BServiceConfig {
    /**
     * Enables debugging of service loading.
     *
     * This includes the operation type (check/create), the run time, the type of the service and where it comes from.
     */
    val debug: Boolean

    @Deprecated(message = "For removal, didn't do much in the first place")
    val serviceAnnotations: Set<KClass<out Annotation>>
    val instanceSupplierMap: Map<KClass<*>, InstanceSupplier<*>>
}

@ConfigDSL
class BServiceConfigBuilder internal constructor() : BServiceConfig {
    override var debug: Boolean = false

    @Deprecated("For removal, didn't do much in the first place")
    override val serviceAnnotations: MutableSet<KClass<out Annotation>> = hashSetOf(BService::class, Command::class, Resolver::class, ResolverFactory::class, Handler::class)

    private val _instanceSupplierMap: MutableMap<KClass<*>, InstanceSupplier<*>> = hashMapOf()
    override val instanceSupplierMap: Map<KClass<*>, InstanceSupplier<*>> = _instanceSupplierMap.unmodifiableView()

    /**
     * Registers a supplier lazily returning an instance of the specified class,
     * the instance is then made available via dependency injection.
     *
     * The class it is **registered as** ([T]) is searched for the usual annotations
     * such as [@Primary][Primary], [@InterfacedService][InterfacedService] and [@Lazy][Lazy].
     *
     * **Note:** The class still needs to be in the search path,
     * either using [BConfigBuilder.addSearchPath] or [BConfigBuilder.addClass].
     *
     * @param clazz            The primary type as which the service is registered as, other types may be registered with the usual annotations
     * @param instanceSupplier Supplier for the service instance, ran at startup, unless [clazz] is annotated with [@Lazy][Lazy]
     */
    fun <T : Any> registerInstanceSupplier(clazz: Class<T>, instanceSupplier: InstanceSupplier<T>) {
        _instanceSupplierMap[clazz.kotlin] = instanceSupplier
    }

    @JvmSynthetic
    internal fun build() = object : BServiceConfig {
        override val debug = this@BServiceConfigBuilder.debug
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override val serviceAnnotations = this@BServiceConfigBuilder.serviceAnnotations.toImmutableSet()
        override val instanceSupplierMap = this@BServiceConfigBuilder.instanceSupplierMap.toImmutableMap()
    }
}

/**
 * Registers a supplier lazily returning an instance of the specified class,
 * the instance is then made available via dependency injection.
 *
 * The class it is **registered as** ([T]) is searched for the usual annotations
 * such as [@Primary][Primary], [@InterfacedService][InterfacedService] and [@Lazy][Lazy].
 *
 * **Note:** The class still needs to be in the search path,
 * either using [BConfigBuilder.addSearchPath] or [BConfigBuilder.addClass].
 *
 * @param T                The primary type as which the service is registered as, other types may be registered with the usual annotations
 * @param instanceSupplier Supplier for the service instance, ran at startup, unless [T] is annotated with [@Lazy][Lazy]
 */
inline fun <reified T : Any> BServiceConfigBuilder.registerInstanceSupplier(instanceSupplier: InstanceSupplier<T>) {
    return registerInstanceSupplier(T::class.java, instanceSupplier)
}
