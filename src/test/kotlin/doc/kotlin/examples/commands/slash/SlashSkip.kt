package doc.kotlin.examples.commands.slash

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.provider.GlobalApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GlobalApplicationCommandProvider
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.ratelimit.RateLimitScope
import io.github.freya022.botcommands.api.commands.ratelimit.RateLimiter
import io.github.freya022.botcommands.api.commands.ratelimit.bucket.Buckets
import io.github.freya022.botcommands.api.commands.ratelimit.bucket.toSupplier
import io.github.freya022.botcommands.test.switches.TestLanguage
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestLanguage(TestLanguage.Language.KOTLIN)
@Command
class SlashSkip : GlobalApplicationCommandProvider {
    suspend fun onSlashSkip(event: GuildSlashEvent) {
        event.reply_("Skipped", ephemeral = true).await().deleteOriginal().await()
    }

    override fun declareGlobalApplicationCommands(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("skip", function = ::onSlashSkip) {
            val bucketConfiguration = Buckets.spikeProtected(
                capacity = 5,
                duration = 1.minutes,
                spikeCapacity = 2,
                spikeDuration = 5.seconds
            )

            rateLimit(RateLimiter.createDefault(RateLimitScope.USER, bucketConfiguration.toSupplier(), deleteOnRefill = true))
        }
    }
}