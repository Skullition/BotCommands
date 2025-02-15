package io.github.freya022.botcommands.internal.components.controller

import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.ratelimit.CancellableRateLimit
import io.github.freya022.botcommands.api.components.ComponentInteractionFilter
import io.github.freya022.botcommands.api.components.ComponentInteractionRejectionHandler
import io.github.freya022.botcommands.api.components.Components
import io.github.freya022.botcommands.api.components.annotations.RequiresComponents
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.components.event.EntitySelectEvent
import io.github.freya022.botcommands.api.components.event.StringSelectEvent
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.Filter
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.checkFilters
import io.github.freya022.botcommands.api.core.config.BComponentsConfigBuilder
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.simpleNestedName
import io.github.freya022.botcommands.api.localization.DefaultMessagesFactory
import io.github.freya022.botcommands.internal.commands.ratelimit.handler.RateLimitHandler
import io.github.freya022.botcommands.internal.components.data.ActionComponentData
import io.github.freya022.botcommands.internal.components.data.PersistentComponentData
import io.github.freya022.botcommands.internal.components.handler.ComponentHandlerExecutor
import io.github.freya022.botcommands.internal.core.ExceptionHandler
import io.github.freya022.botcommands.internal.localization.interaction.LocalizableInteractionFactory
import io.github.freya022.botcommands.internal.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException

private val logger = KotlinLogging.logger { }

@BService
@RequiresComponents
internal class ComponentsListener(
    private val context: BContext,
    private val defaultMessagesFactory: DefaultMessagesFactory,
    private val localizableInteractionFactory: LocalizableInteractionFactory,
    private val rateLimitHandler: RateLimitHandler,
    filters: List<ComponentInteractionFilter<*>>,
    rejectionHandler: ComponentInteractionRejectionHandler<*>?,
    private val componentController: ComponentController,
    private val continuationManager: ComponentContinuationManager,
    private val componentHandlerExecutor: ComponentHandlerExecutor,
) {
    private val scope = context.coroutineScopesConfig.componentScope
    private val exceptionHandler = ExceptionHandler(context, logger)

    // Types are crosschecked anyway
    @Suppress("UNCHECKED_CAST")
    private val globalFilters = filters.filter { it.global } as List<ComponentInteractionFilter<Any>>
    @Suppress("UNCHECKED_CAST")
    private val rejectionHandler = when {
        globalFilters.isEmpty() -> null
        else -> rejectionHandler as ComponentInteractionRejectionHandler<Any>?
            ?: throwState("A ${classRef<ComponentInteractionRejectionHandler<*>>()} must be available if ${classRef<ComponentInteractionFilter<*>>()} is used")
    }

    @BEventListener
    internal fun onComponentInteraction(event: GenericComponentInteractionCreateEvent) {
        logger.trace { "Received ${event.componentType} interaction: ${event.component}" }

        scope.launchCatching({ handleException(event, it) }) launch@{
            val componentId = event.componentId.let { id ->
                if (!ComponentController.isCompatibleComponent(id))
                    return@launch logger.error { "Received an interaction for an external component format: '${event.componentId}', " +
                            "please only use ${classRef<Components>()} to make components or disable ${BComponentsConfigBuilder::enable.reference}" }
                ComponentController.parseComponentId(id)
            }
            val component = componentController.getActiveComponent(componentId)
                ?: return@launch event.reply_(defaultMessagesFactory.get(event).componentExpiredErrorMsg, ephemeral = true).queue()

            if (component !is ActionComponentData)
                throwInternal("Somehow retrieved a non-executable component on a component interaction: $component")

            if (component.filters === ComponentFilters.INVALID_FILTERS) {
                return@launch event.reply_(defaultMessagesFactory.get(event).componentNotAllowedErrorMsg, ephemeral = true).queue()
            }

            component.filters.onEach { filter ->
                require(!filter.global) {
                    "Global filter ${filter.javaClass.simpleNestedName} cannot be used explicitly, see ${Filter::global.reference}"
                }
            }

            rateLimitHandler.tryRun(component, event) { cancellableRateLimit ->
                val enhancedEvent = transformEvent(event, cancellableRateLimit)
                onComponentUse(enhancedEvent, component)
            }
        }
    }

    private fun transformEvent(
        event: GenericComponentInteractionCreateEvent,
        cancellableRateLimit: CancellableRateLimit
    ): GenericComponentInteractionCreateEvent {
        val localizableInteraction = localizableInteractionFactory.create(event)
        return when (event) {
            is ButtonInteractionEvent -> ButtonEvent(context, event, cancellableRateLimit, localizableInteraction)
            is StringSelectInteractionEvent -> StringSelectEvent(context, event, cancellableRateLimit, localizableInteraction)
            is EntitySelectInteractionEvent -> EntitySelectEvent(context, event, cancellableRateLimit, localizableInteraction)
            else -> throwInternal("Unhandled component event: ${event::class.simpleName}")
        }
    }

    private suspend fun onComponentUse(
        event: GenericComponentInteractionCreateEvent,
        component: ActionComponentData
    ): Boolean {
        if (!component.constraints.isAllowed(event)) {
            event.reply_(defaultMessagesFactory.get(event).componentNotAllowedErrorMsg, ephemeral = true).queue()
            return false
        }

        checkFilters(globalFilters, component.filters) { filter ->
            val handlerName = (component as? PersistentComponentData)?.handler?.handlerName
            val userError = filter.checkSuspend(event, handlerName)
            if (userError != null) {
                rejectionHandler!!.handleSuspend(event, handlerName, userError)
                if (event.isAcknowledged) {
                    logger.trace { "${filter::class.simpleNestedName} rejected ${event.componentType} interaction (handler: ${component.handler})" }
                } else {
                    logger.error { "${filter::class.simpleNestedName} rejected ${event.componentType} interaction (handler: ${component.handler}) but did not acknowledge the interaction" }
                }
                return false
            }
        }

        // Resume coroutines before deleting the component,
        // as it will also delete the continuations (that we already consume anyway)
        continuationManager.resumeCoroutines(component, event)

        if (component.singleUse) {
            // No timeout will be thrown as all continuations have been resumed.
            // So, a timeout being thrown is an issue.
            componentController.deleteComponent(component, throwTimeouts = true)
        } else {
            componentController.tryResetTimeout(component)
        }

        return componentHandlerExecutor.runHandler(component, event)
    }

    private suspend fun handleException(event: GenericComponentInteractionCreateEvent, e: Throwable) {
        exceptionHandler.handleException(event, e, "component interaction, ID: '${event.componentId}'", mapOf(
            "Message" to event.message.jumpUrl,
            "Component" to event.component
        ))
        if (e is InsufficientPermissionException) {
            event.replyExceptionMessage(defaultMessagesFactory.get(event).getBotPermErrorMsg(setOf(e.permission)))
        } else {
            event.replyExceptionMessage(defaultMessagesFactory.get(event).generalErrorMsg)
        }
    }
}