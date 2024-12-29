package io.github.freya022.botcommands.framework

import io.github.freya022.botcommands.api.core.BotCommands
import io.github.freya022.botcommands.api.core.config.BConfigBuilder
import io.github.freya022.botcommands.api.core.service.getService
import io.github.freya022.botcommands.api.emojis.AppEmojisRegistry
import io.github.freya022.botcommands.api.emojis.annotations.AppEmojiContainer
import io.github.freya022.botcommands.api.emojis.exceptions.EmojiAlreadyExistsException
import io.github.freya022.botcommands.api.emojis.exceptions.NoEmojiResourceException
import io.github.freya022.botcommands.api.emojis.exceptions.NonUniqueEmojiResourceException
import io.github.freya022.botcommands.internal.emojis.AppEmojisLoader
import io.mockk.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

private const val EXAMPLE_BASE_PATH = "/my_emojis"
private const val EXAMPLE_BASE_PATH_2 = "/my_other_emojis"
private const val EXAMPLE_ASSET_PATTERN = "my_asset.**"
private const val EXAMPLE_EMOJI_NAME = "my_emoji-1"
private const val TEST_IDENTIFIER = "ident"

abstract class AbstractAppEmojisTest {

    @BeforeEach
    fun setup() {
        AppEmojisLoader.clear()
    }

    protected fun light(block: BConfigBuilder.() -> Unit) = BotCommands.create {
        disableExceptionsInDMs = true

        components {
            enable = false
        }

        textCommands {
            enable = false
        }

        applicationCommands {
            enable = false
        }

        modals {
            enable = false
        }

        appEmojis {
            enable = true
        }

        addClass<FakeBot>()

        block()
    }

    protected inline fun <reified T : Throwable> unwrapException(block: () -> Unit) {
        return try {
            block()
        } catch (e: Throwable) {
            if (e is T) {
                throw e.cause!!
            } else {
                throw e
            }
        }
    }
}

class AppEmojisTest : AbstractAppEmojisTest() {

    @AppEmojiContainer
    object EagerLazyMix {
        val emoji1: ApplicationEmoji by AppEmojisRegistry
        val emoji2: ApplicationEmoji by AppEmojisRegistry.lazy(EXAMPLE_ASSET_PATTERN, EXAMPLE_EMOJI_NAME)
        val emoji3: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji3)
    }

    @Test
    fun `Mixing eager and lazy app emojis throws IAE`() {
        assertThrows<IllegalArgumentException> {
            unwrapException<RuntimeException> {
                light {
                    addClass<EagerLazyMix>()
                }
            }
        }
    }

    @Test
    fun `Not loaded yet throws ISE`() {
        assertThrows<IllegalStateException> {
            unwrapException<ExceptionInInitializerError> {
                EagerLazyMix.emoji1
            }
        }
    }

    object NotEmojiContainer {
        val emoji1: ApplicationEmoji by AppEmojisRegistry
    }

    @Test
    fun `Not called by AppEmojiContainer throws ICE`() {
        assertThrows<IllegalCallerException> {
            NotEmojiContainer.emoji1
        }
    }

    @Test
    fun `Check prerequisites`() {
        assertDoesNotThrow { AppEmojisLoader.register(EXAMPLE_BASE_PATH, EXAMPLE_ASSET_PATTERN, EXAMPLE_EMOJI_NAME, TEST_IDENTIFIER) }

        assertThrows<IllegalArgumentException> { AppEmojisLoader.register(basePath = "base", EXAMPLE_ASSET_PATTERN, EXAMPLE_EMOJI_NAME, TEST_IDENTIFIER) }
        assertThrows<IllegalArgumentException> { AppEmojisLoader.register(basePath = "base/", EXAMPLE_ASSET_PATTERN, EXAMPLE_EMOJI_NAME, TEST_IDENTIFIER) }

        assertThrows<IllegalArgumentException> { AppEmojisLoader.register(EXAMPLE_BASE_PATH, EXAMPLE_ASSET_PATTERN, emojiName = "a", TEST_IDENTIFIER) }
        assertThrows<IllegalArgumentException> { AppEmojisLoader.register(EXAMPLE_BASE_PATH, EXAMPLE_ASSET_PATTERN, emojiName = "abc$", TEST_IDENTIFIER) }
    }

    @Test
    fun `Can't register multiple emojis of same name`() {
        assertDoesNotThrow { AppEmojisLoader.register(EXAMPLE_BASE_PATH, EXAMPLE_ASSET_PATTERN, EXAMPLE_EMOJI_NAME, TEST_IDENTIFIER) }
        assertThrows<EmojiAlreadyExistsException> { AppEmojisLoader.register(EXAMPLE_BASE_PATH, EXAMPLE_ASSET_PATTERN, EXAMPLE_EMOJI_NAME, TEST_IDENTIFIER) }
    }

    @AppEmojiContainer
    object MultipleCandidates {
        val emoji1: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji1, assetPattern = "**")
    }

    @Test
    fun `Multiple resource candidates throws`() {
        val context = light {
            addClass<MultipleCandidates>()
        }
        val loader = context.getService<AppEmojisLoader>()

        val jda = mockk<JDA> {
            every { retrieveApplicationEmojis().complete() } returns emptyList()
        }
        assertThrows<NonUniqueEmojiResourceException> {
            loader.loadEmojis(jda)
        }
    }

    @AppEmojiContainer
    object NoCandidate {
        val emoji1: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji1, assetPattern = "blah")
    }

    @Test
    fun `No resource candidates throws`() {
        val context = light {
            addClass<NoCandidate>()
        }
        val loader = context.getService<AppEmojisLoader>()

        val jda = mockk<JDA> {
            every { retrieveApplicationEmojis().complete() } returns emptyList()
        }
        assertThrows<NoEmojiResourceException> {
            loader.loadEmojis(jda)
        }
    }

    @AppEmojiContainer
    object SingleCandidate {
        val kotlin: ApplicationEmoji by AppEmojisRegistry.lazy(::kotlin)
    }

    @Test
    fun `Single resource candidate`() {
        val context = light {
            addClass<SingleCandidate>()
        }
        val loader = context.getService<AppEmojisLoader>()

        val jda = mockk<JDA> {
            every { retrieveApplicationEmojis().complete() } returns emptyList()
            every { createApplicationEmoji(any(), any()).complete() } returns mockk()
        }
        assertDoesNotThrow {
            loader.loadEmojis(jda)
        }
    }
}

class AppEmojiRegistrationValuesTest : AbstractAppEmojisTest() {

    @AppEmojiContainer(EXAMPLE_BASE_PATH)
    object AnnotationBasePath {
        val emoji1: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji1)
    }

    @AppEmojiContainer(EXAMPLE_BASE_PATH)
    object CustomBasePath {
        val emoji1: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji1, basePath = EXAMPLE_BASE_PATH_2)
    }

    @MethodSource("Base paths")
    @ParameterizedTest
    fun `Base path optional override`(containerType: Class<*>, expectedBasePath: String) {
        mockkObject(AppEmojisLoader) {
            val basePath = slot<String>()
            every { AppEmojisLoader.register(capture(basePath), any(), any(), any()) } just runs

            light {
                addClass(containerType)
            }

            assertEquals(expectedBasePath, basePath.captured)
        }
    }

    @AppEmojiContainer
    object DefaultAssetPattern {
        val emoji1: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji1)
    }

    @AppEmojiContainer
    object CustomAssetPattern {
        val emoji1: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji1, assetPattern = EXAMPLE_ASSET_PATTERN)
    }

    @MethodSource("Asset patterns")
    @ParameterizedTest
    fun `Asset pattern optional override`(containerType: Class<*>, expectedAssetPattern: String) {
        mockkObject(AppEmojisLoader) {
            val assetPattern = slot<String>()
            every { AppEmojisLoader.register(any(), capture(assetPattern), any(), any()) } just runs

            light {
                addClass(containerType)
            }

            assertEquals(expectedAssetPattern, assetPattern.captured)
        }
    }

    @AppEmojiContainer
    object DefaultEmojiName {
        val emoji1: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji1)
    }

    @AppEmojiContainer
    object CustomEmojiName {
        val emoji1: ApplicationEmoji by AppEmojisRegistry.lazy(::emoji1, emojiName = EXAMPLE_EMOJI_NAME)
    }

    @MethodSource("Emoji names")
    @ParameterizedTest
    fun `Emoji name optional override`(containerType: Class<*>, expectedEmojiName: String) {
        mockkObject(AppEmojisLoader) {
            val emojiName = slot<String>()
            every { AppEmojisLoader.register(any(), any(), capture(emojiName), any()) } just runs

            light {
                addClass(containerType)
            }

            assertEquals(expectedEmojiName, emojiName.captured)
        }
    }

    companion object {
        @JvmStatic
        fun `Base paths`(): List<Arguments> = listOf(
            Arguments.of(AnnotationBasePath::class.java, EXAMPLE_BASE_PATH),
            Arguments.of(CustomBasePath::class.java, EXAMPLE_BASE_PATH_2),
        )

        @JvmStatic
        fun `Asset patterns`(): List<Arguments> = listOf(
            Arguments.of(DefaultAssetPattern::class.java, "emoji1.**"),
            Arguments.of(CustomAssetPattern::class.java, EXAMPLE_ASSET_PATTERN),
        )

        @JvmStatic
        fun `Emoji names`(): List<Arguments> = listOf(
            Arguments.of(DefaultEmojiName::class.java, "emoji1"),
            Arguments.of(CustomEmojiName::class.java, EXAMPLE_EMOJI_NAME),
        )
    }
}