package io.github.freya022.botcommands.api.emojis

import io.github.freya022.botcommands.api.core.utils.findAnnotationRecursive
import io.github.freya022.botcommands.api.core.utils.hasAnnotationRecursive
import io.github.freya022.botcommands.api.core.utils.simpleNestedName
import io.github.freya022.botcommands.api.emojis.AppEmojisRegistry.get
import io.github.freya022.botcommands.api.emojis.AppEmojisRegistry.getValue
import io.github.freya022.botcommands.api.emojis.annotations.AppEmojiContainer
import io.github.freya022.botcommands.internal.emojis.AppEmojisLoader
import io.github.freya022.botcommands.internal.utils.*
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji
import java.util.*
import kotlin.reflect.KProperty

/**
 * Registry containing [ApplicationEmoji], you can get one directly or using lazy property delegates.
 *
 * Using these methods requires the caller class to be annotated with [@AppEmojiContainer][AppEmojiContainer].
 *
 * @see AppEmojiContainer
 */
object AppEmojisRegistry {

    /**
     * Returns the emoji loaded for the field named [fieldName].
     *
     * @param fieldName Name of the current field
     *
     * @throws IllegalCallerException   If [@AppEmojiContainer][AppEmojiContainer] is not found in the call stack
     * @throws IllegalStateException    If the emojis were not loaded yet
     * @throws IllegalArgumentException If no emoji could be found for this field name
     */
    @JvmStatic
    operator fun get(fieldName: String): ApplicationEmoji {
        val callerClass =
            stackWalker.find { it.declaringClass.kotlin.hasAnnotationRecursive<AppEmojiContainer>() }?.declaringClass
                ?: throw IllegalCallerException("This method can only be called by a ${annotationRef<AppEmojiContainer>()} class or any class in the call stack")
        return AppEmojisLoader.getByIdentifierOrNull("${callerClass.simpleNestedName}.${fieldName}")
            ?: throwArgument("Could not find emoji field named '$fieldName', did you forget ${annotationRef<AppEmojiContainer>()}?")
    }

    /**
     * Returns the emoji loaded for the current property.
     *
     * This is effectively equivalent to `= AppEmojisRegistry.get("myPropertyName")`.
     *
     * @throws IllegalCallerException   If [@AppEmojiContainer][AppEmojiContainer] is not found in the call stack
     * @throws IllegalStateException    If the emojis were not loaded yet
     * @throws IllegalArgumentException If no emoji could be found for this property name
     */
    @JvmSynthetic
    operator fun getValue(thisObj: Any, property: KProperty<*>): ApplicationEmoji {
        return get(property.name)
    }

    /**
     * Lazily retrieves an emoji named [emojiName], created from the [assetName] found in the `basePath` of your class.
     *
     * The `basePath` is taken from the first [@AppEmojiContainer][AppEmojiContainer] found in the call stack,
     * typically the caller of this function.
     *
     * This cannot be used alongside non-lazy methods ([get] and [getValue]).
     *
     * @param assetName The name of the file to be searched for, including the extension
     * @param emojiName The name of the emoji uploaded on Discord,
     * defaults to [assetName] without its extension and converted from `camelCase` to `snake_case`,
     * must be between 2 and [EMOJI_NAME_MAX_LENGTH][ApplicationEmoji.EMOJI_NAME_MAX_LENGTH] and only have alphanumerics with dashes
     *
     * @throws IllegalCallerException   If [@AppEmojiContainer][AppEmojiContainer] is not found in the call stack
     * @throws IllegalArgumentException If the `basePath` starts with a `/`
     * @throws IllegalArgumentException If the `basePath` ends with a `/`
     * @throws IllegalArgumentException If [emojiName] is too long or too short
     * @throws IllegalArgumentException If [emojiName] has invalid characters
     * @throws IllegalArgumentException If an emoji with the same name was already registered
     * @throws IllegalStateException    If the emojis were already loaded
     */
    @JvmSynthetic
    fun lazy(
        assetPattern: String,
        // Don't provide a default value; the user could think the default is the property name, when it isn't
        emojiName: String,
    ): Lazy<ApplicationEmoji> {
        val annotation =
            stackWalker.firstNotNullOfOrNull { it.declaringClass.kotlin.findAnnotationRecursive<AppEmojiContainer>() }
                ?: throw IllegalCallerException("This method can only be called by a ${annotationRef<AppEmojiContainer>()} class or any class in the call stack")
        return lazy(annotation.basePath, assetName, emojiName)
    }

    /**
     * Lazily retrieves an emoji named [emojiName], created from the [assetName] found in the [basePath].
     *
     * This cannot be used alongside non-lazy methods ([get] and [getValue]).
     *
     * @param basePath  Path at which the file can be searched in; must start with a `/` and NOT end with a `/`
     * @param assetName The name of the file to be searched for, including the extension
     * @param emojiName The name of the emoji uploaded on Discord,
     * defaults to [assetName] without its extension and converted from `camelCase` to `snake_case`,
     * must be between 2 and [EMOJI_NAME_MAX_LENGTH][ApplicationEmoji.EMOJI_NAME_MAX_LENGTH] and only have alphanumerics with dashes
     *
     * @throws IllegalArgumentException If [basePath] starts with a `/`
     * @throws IllegalArgumentException If [basePath] ends with a `/`
     * @throws IllegalArgumentException If [emojiName] is too long or too short
     * @throws IllegalArgumentException If [emojiName] has invalid characters
     * @throws IllegalArgumentException If an emoji with the same name was already registered
     * @throws IllegalStateException    If the emojis were already loaded
     */
    @JvmSynthetic
    fun lazy(
        basePath: String,
        assetPattern: String,
        // Don't provide a default value; the user could think the default is the property name, when it isn't
        emojiName: String,
    ): Lazy<ApplicationEmoji> {
        val identifier = UUID.randomUUID().toString()
        AppEmojisLoader.register(basePath, assetName, emojiName, identifier)
        return lazy {
            AppEmojisLoader.getByIdentifierOrNull(identifier)
                ?: throwInternal("Could not get back emoji '$emojiName' from UUID")
        }
    }
}