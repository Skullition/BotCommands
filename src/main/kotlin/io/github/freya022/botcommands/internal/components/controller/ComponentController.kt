package io.github.freya022.botcommands.internal.components.controller

import io.github.freya022.botcommands.api.commands.ratelimit.declaration.RateLimitProvider
import io.github.freya022.botcommands.api.components.ComponentGroup
import io.github.freya022.botcommands.api.components.ComponentInteractionFilter
import io.github.freya022.botcommands.api.components.annotations.RequiresComponents
import io.github.freya022.botcommands.api.components.ratelimit.ComponentRateLimitReference
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.Filter
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.service.lazy
import io.github.freya022.botcommands.api.core.utils.simpleNestedName
import io.github.freya022.botcommands.internal.commands.ratelimit.RateLimitContainer
import io.github.freya022.botcommands.internal.components.builder.group.AbstractComponentGroupBuilder
import io.github.freya022.botcommands.internal.components.builder.mixin.BaseComponentBuilderMixin
import io.github.freya022.botcommands.internal.components.data.ActionComponentData
import io.github.freya022.botcommands.internal.components.data.ComponentData
import io.github.freya022.botcommands.internal.components.handler.EphemeralComponentHandlers
import io.github.freya022.botcommands.internal.components.repositories.ComponentRepository
import io.github.freya022.botcommands.internal.components.timeout.EphemeralTimeoutHandlers
import io.github.freya022.botcommands.internal.utils.classRef
import io.github.freya022.botcommands.internal.utils.reference
import io.github.freya022.botcommands.internal.utils.takeIfFinite
import io.github.freya022.botcommands.internal.utils.throwInternal
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

private const val PREFIX = "BotCommands-Components-"
private const val PREFIX_LENGTH = PREFIX.length

private val logger = KotlinLogging.logger { }

@BService
@RequiresComponents
internal class ComponentController(
    val context: BContext,
    internal val continuationManager: ComponentContinuationManager,
    private val componentRepository: ComponentRepository,
    private val ephemeralComponentHandlers: EphemeralComponentHandlers,
    private val ephemeralTimeoutHandlers: EphemeralTimeoutHandlers,
    private val timeoutManager: ComponentTimeoutManager
) {
    // This service might be used in classes that use components and also declare rate limiters
    private val rateLimitContainer: RateLimitContainer by context.serviceContainer.lazy()
    private val rateLimitReferences: MutableSet<ComponentRateLimitReference> = hashSetOf()

    init {
        runBlocking {
            removeEphemeralComponents()
            scheduleExistingTimeouts()
        }
    }

    private suspend fun removeEphemeralComponents() {
        val removedComponents = componentRepository.removeEphemeralComponents()
        logger.debug { "Removed $removedComponents ephemeral components" }
    }

    private suspend fun scheduleExistingTimeouts() {
        componentRepository
            .getPersistentComponentTimeouts()
            .forEach {
                timeoutManager.scheduleTimeout(it.componentId, it.instant)
            }
    }

    internal suspend inline fun <R> withNewComponent(builder: BaseComponentBuilderMixin<*>, block: (internalId: Int, componentId: String) -> R): R {
        val internalId = createComponent(builder).internalId
        return block(internalId, getComponentId(internalId))
    }

    private suspend fun createComponent(builder: BaseComponentBuilderMixin<*>): ComponentData {
        builder.rateLimitReference?.let { rateLimitReference ->
            require(rateLimitReference.group in rateLimitContainer) {
                "Rate limit group '${rateLimitReference.group}' was not registered using ${classRef<RateLimitProvider>()}"
            }
        }

        builder.filters.onEach { filter ->
            val filterClass = filter.javaClass
            require(!filter.global) {
                "Global filter ${filterClass.simpleNestedName} cannot be used explicitly, see ${Filter::global.reference}"
            }

            requireNotNull(context.serviceContainer.getServiceOrNull(filterClass)) {
                "Component filters must be accessible via dependency injection, " +
                        "filters such as composite filters created with 'and' / 'or' cannot be passed. " +
                        "See ${classRef<ComponentInteractionFilter<*>>()} for more details."
            }
        }

        if (builder.resetTimeoutOnUse && builder.timeoutDuration?.takeIfFinite() == null) {
            logger.warn { "Using 'resetTimeoutOnUse' has no effect when no timeout is set" }
        }

        val component = componentRepository.createComponent(builder)

        component.expiresAt?.let { expirationTimestamp ->
            timeoutManager.scheduleTimeout(component.internalId, expirationTimestamp)
        }

        return component
    }

    internal suspend fun getActiveComponent(componentId: Int): ComponentData? {
        return componentRepository.getComponent(componentId)
            ?.takeUnless {
                val expiresAt = it.expiresAt
                expiresAt != null && expiresAt <= Clock.System.now()
            }
    }

    internal suspend fun tryResetTimeout(component: ComponentData) {
        // Components in groups cannot have timeouts,
        // so if there's a group, only reset the group timeout
        val group = (component as? ActionComponentData)?.group
        if (group != null) {
            tryResetTimeout(group)
        } else {
            if (component.resetTimeoutOnUseDuration == null) return

            // Cancel, reset in DB, schedule
            timeoutManager.cancelTimeout(component.internalId)
            val newExpirationTimestamp = componentRepository.resetExpiration(component.internalId)
                ?: throwInternal("New expiration timestamp is null despite ${component::resetTimeoutOnUseDuration.reference} being non-null")
            timeoutManager.scheduleTimeout(component.internalId, newExpirationTimestamp)
        }
    }

    suspend fun deleteComponent(component: ComponentData, throwTimeouts: Boolean) =
        deleteComponentsById(listOf(component.internalId), throwTimeouts)

    suspend fun createGroup(builder: AbstractComponentGroupBuilder<*>): ComponentGroup {
        val group = componentRepository.insertGroup(builder)

        group.expiresAt?.let { expirationTimestamp ->
            timeoutManager.scheduleTimeout(group.internalId, expirationTimestamp)
        }

        return ComponentGroup(this, group.internalId)
    }

    suspend fun deleteComponentsById(ids: Collection<Int>, throwTimeouts: Boolean) {
        componentRepository.deleteComponentsById(ids).forEach { (componentId, ephemeralComponentHandlerId, ephemeralTimeoutHandlerId) ->
            ephemeralComponentHandlerId?.let { ephemeralComponentHandlers.remove(it) }
            ephemeralTimeoutHandlerId?.let { ephemeralTimeoutHandlers.remove(it) }
            timeoutManager.removeTimeouts(componentId, throwTimeouts)
        }
    }

    internal fun createRateLimitReference(group: String, discriminator: String): ComponentRateLimitReference {
        val ref = ComponentRateLimitReference(group, discriminator)
        check(rateLimitReferences.add(ref)) {
            "A component rate limit reference already exists with such group and discriminator. " +
                    "As a reminder, each component must use a different discriminator."
        }
        return ref
    }

    internal fun getRateLimitReference(group: String, discriminator: String): ComponentRateLimitReference? {
        val ref = ComponentRateLimitReference(group, discriminator)
        return ref.takeIf { ref in rateLimitReferences }
    }

    internal companion object {
        internal fun isCompatibleComponent(id: String): Boolean = id.startsWith(PREFIX)

        internal fun parseComponentId(id: String): Int = Integer.parseInt(id, PREFIX_LENGTH, id.length, 10)

        internal fun getComponentId(internalId: Int): String = PREFIX + internalId
    }
}