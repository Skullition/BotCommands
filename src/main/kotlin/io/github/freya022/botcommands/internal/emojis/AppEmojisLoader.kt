package io.github.freya022.botcommands.internal.emojis

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.annotations.BEventListener.RunMode
import io.github.freya022.botcommands.api.core.events.PreFirstGatewayConnectEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.*
import io.github.freya022.botcommands.api.emojis.AppEmojisRegistry
import io.github.freya022.botcommands.api.emojis.annotations.AppEmoji
import io.github.freya022.botcommands.api.emojis.annotations.AppEmojiContainer
import io.github.freya022.botcommands.api.emojis.annotations.RequiresAppEmojis
import io.github.freya022.botcommands.internal.emojis.AppEmojisLoader.Companion.register
import io.github.freya022.botcommands.internal.utils.annotationRef
import io.github.freya022.botcommands.internal.utils.putIfAbsentOrThrowInternal
import io.github.freya022.botcommands.internal.utils.toDiscordString
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji
import net.dv8tion.jda.internal.utils.Checks
import kotlin.collections.set
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

private val logger = KotlinLogging.logger { }

/**
 * The [register] function is the core of the loading mechanism,
 * by adding a set of basePath/assetName/emojiName/identifier,
 * the [retriever/uploader][onPreGatewayConnect] can then create the registered emojis,
 * and then assign them to a map of loaded emojis, with the key being the "identifier".
 *
 * When an application emoji container is detected,
 * its fields are reads, can be optionally customized with [@AppEmoji][AppEmoji],
 * and are then registered to be loaded later.
 *
 * Kotlin users can alternatively use [AppEmojisRegistry.lazy] to register their emojis lazily,
 * but cannot mix those with other retrieval methods in the same class.
 *
 * So only when the user requires the application emojis, they are fetched from the loaded emojis.
 */
@BService
@RequiresAppEmojis
internal class AppEmojisLoader internal constructor(

) {

    init {
        val emojiContainers = AppEmojiContainerProcessor.emojiClasses
        logger.trace { "Registering app emoji containers for pre-login retrieval:\n${emojiContainers.joinAsList { it.clazz.shortQualifiedName }}" }

        val packages = emojiContainers.map { it.annotation.basePath }
        if (packages.isNotEmpty()) {
            ClassGraph()
                .acceptPackages(*packages.toTypedArray())
                .scan()
                .use { scan ->
                    for (emojiContainer in emojiContainers) {
                        processClass(emojiContainer, scan)
                    }
                }
        }
    }

    private fun processClass(emojiContainer: AppEmojiContainerData, scan: ScanResult) {
        val clazz = emojiContainer.clazz
        val basePath = emojiContainer.annotation.basePath

        // Separate lazy from eager properties
        // Can't mix both as lazy properties registration would trigger loading of eager properties
        val (lazyProperties, eagerProperties) = (clazz.staticProperties + clazz.declaredMemberProperties)
            .filter { it.returnType.jvmErasure == ApplicationEmoji::class }
            .partition { it.javaField?.type == Lazy::class.java }

        if (lazyProperties.isNotEmpty()) {
            require(eagerProperties.isEmpty()) {
                "Cannot mix lazy and eager properties in '${clazz.simpleNestedName}', eager properties: ${eagerProperties.joinToString { it.name }}"
            }

            clazz.objectInstance // Force initialization (and thus registration)
            return
        }

        eagerProperties.forEach { property ->
            val fieldAnnotation = property.findAnnotationRecursive<AppEmoji>()
            val snakeCaseFieldName = when {
                // If screaming case, only take lowercase
                property.name.filter { it.isLetter() }.all { it.isUpperCase() } -> property.name.lowercase()
                // Else, consider camelCase
                else -> property.name.toDiscordString()
            }
            // Use annotation name if not default
            val emojiName = fieldAnnotation?.emojiName
                ?.takeIf { it !== AppEmoji.DEFAULT }
                ?: snakeCaseFieldName

            val resourceName = when {
                fieldAnnotation != null && fieldAnnotation.assetName !== AppEmoji.DEFAULT -> fieldAnnotation.assetName

                else -> "$snakeCaseFieldName.**"
            }
            // CG doesn't need / as root
            val wildcardString = "${basePath.drop(1)}/$resourceName"
            val resources = scan.getResourcesMatchingWildcard(wildcardString)
            require(resources.isNotEmpty()) {
                "Found no resources for ${clazz.simpleNestedName}.${property.name}, matching '$wildcardString'"
            }
            require(resources.size == 1) {
                "Found multiple resources for ${clazz.simpleNestedName}.${property.name}: ${resources.joinToString { it.pathRelativeToClasspathElement }}"
            }

            val assetName = resources.single().path.substringAfterLast('/')
            register(basePath, assetName, emojiName, "${clazz.simpleNestedName}.${property.name}")
        }
    }

    @BEventListener(mode = RunMode.BLOCKING)
    internal fun onPreGatewayConnect(event: PreFirstGatewayConnectEvent) {
        if (toLoad.isEmpty()) return logger.debug { "No application emojis to load" }

        logger.debug { "Fetching application emojis" }

        val applicationEmojis = event.jda.retrieveApplicationEmojis().complete()

        val missingRequests = arrayListOf<LoadRequest>()
        toLoad.forEach { request ->
            val appEmoji = applicationEmojis.find { it.name == request.emojiName }
            if (appEmoji != null) {
                loadedEmojis[request.identifier] = appEmoji
            } else {
                missingRequests += request
            }
        }

        if (missingRequests.isEmpty()) {
            logger.debug { "Application emojis loaded, none were created" }
            loaded = true
            return
        }

        logger.info { "${missingRequests.size} application emojis are missing, this may take a while." }
        for ((basePath, assetName, emojiName, identifier) in missingRequests) {
            val icon = withResource("$basePath/$assetName", Icon::from)
            loadedEmojis.putIfAbsentOrThrowInternal(identifier, event.jda.createApplicationEmoji(emojiName, icon).complete())
        }

        logger.info { "Application emojis loaded, ${missingRequests.size} were created" }
        loaded = true
    }

    // Identifier is the field name for annotation usages, UUID for "manual" registration
    private data class LoadRequest(val basePath: String, val assetName: String, val emojiName: String, val identifier: String)

    internal companion object {

        internal var loaded = false
            private set
        private val toLoadEmojiNames = hashSetOf<String>()
        private val toLoad = arrayListOf<LoadRequest>()
        private val loadedEmojis = hashMapOf<String, ApplicationEmoji>()

        internal fun getByIdentifierOrNull(identifier: String): ApplicationEmoji? {
            check(loaded) {
                "Emojis were not loaded yet, have you enabled the feature? Try to use your app emoji container only when necessary"
            }

            return loadedEmojis[identifier]
        }

        internal fun register(
            basePath: String,
            assetName: String,
            emojiName: String,
            identifier: String,
        ) {
            require(basePath.startsWith("/")) { "The base path must start with '/'" }
            require(!basePath.endsWith("/")) { "The base path must not end with '/'" }
            Checks.inRange(emojiName, 2, ApplicationEmoji.EMOJI_NAME_MAX_LENGTH, "Emoji name")
            Checks.matches(emojiName, Checks.ALPHANUMERIC_WITH_DASH, "Emoji name")

            check(!loaded) {
                "Cannot get an application emoji after they were loaded, did you forget to use ${annotationRef<AppEmojiContainer>()}?"
            }

            require(toLoadEmojiNames.add(emojiName)) { "The emoji name '$emojiName' is already in use." }

            toLoad += LoadRequest(basePath, assetName, emojiName, identifier)
        }
    }
}