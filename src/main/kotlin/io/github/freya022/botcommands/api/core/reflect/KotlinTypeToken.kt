package io.github.freya022.botcommands.api.core.reflect

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

/**
 * Represents a generic type [T].
 *
 * You must make a subclass with the desired type, for example, for a `List<String>`:
 * ```java
 * var typeToken = new KotlinTypeToken<List<String>>() {};
 * ```
 *
 * However, you cannot create a KotlinTypeToken from a type variable (a generic, if you prefer):
 * ```java
 * <T> void myMethod() {
 *     // Doesn't work!
 *     var typeToken = new KotlinTypeToken<T>() {};
 * }
 * ```
 */
open class KotlinTypeToken<T> protected constructor() {

    /**
     * The Kotlin type represented by this instance.
     */
    val type: KType = this::class.supertypes
        // `this` is a subclass of this class, so there must be a supertype of our erasure
        .single { it.jvmErasure == KotlinTypeToken::class }
        // Get the T type of KotlinTypeToken
        .arguments[0].type!!

    init {
        requireNotNull(type.classifier) {
            "Cannot represent '${(this.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]}' as a KotlinTypeToken"
        }
    }
}