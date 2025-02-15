package io.github.freya022.botcommands.test.services

import io.github.freya022.botcommands.api.core.JDAService
import io.github.freya022.botcommands.api.core.events.BReadyEvent
import io.github.freya022.botcommands.api.core.lightSharded
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.enumSetOf
import io.github.freya022.botcommands.test.config.Config
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

@BService
class Bot(private val config: Config, environment: ConfigurableEnvironment?) : JDAService() {
    override val intents: Set<GatewayIntent> =
        defaultIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)

    override val cacheFlags: Set<CacheFlag> =
        enumSetOf(CacheFlag.FORUM_TAGS, CacheFlag.VOICE_STATE)

    init {
        // Copy intents to our Spring environment, so we don't have to duplicate them to application.properties
        environment?.propertySources?.addLast(MapPropertySource("JDAService properties", mapOf(
            "jda.intents" to intents.joinToString(),
            "jda.cacheFlags" to cacheFlags.joinToString(),
        )))
    }

    override fun createJDA(event: BReadyEvent, eventManager: IEventManager) {
        lightSharded(
            token = config.token,
            shardRange = if (config.testMode) 0..1 else null,
            shardsTotal = if (config.testMode) 2 else -1,
            memberCachePolicy = MemberCachePolicy.VOICE,
            activityProvider = { Activity.playing("coroutines go brrr #$it") },
        )
    }
}