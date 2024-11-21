package io.github.freya022.botcommands.internal.core

import io.github.freya022.botcommands.api.core.JDAService
import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.config.JDAConfiguration
import io.github.freya022.botcommands.api.core.events.InjectedJDAEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.internal.utils.reference
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.requests.GatewayIntent
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

@BService
internal object JDAServiceMismatchChecker {
    @BEventListener
    internal fun onJDA(event: InjectedJDAEvent, jdaService: JDAService) {
        val jdaIntents = event.jda.gatewayIntents
        // When JDA renames intents which share the same offset,
        // JDA will report both the old and the new intent as being active,
        // so we need to do the same with the user-specified intents,
        // just for the sake of checking.
        val jdaServiceIntents = jdaService.intents.withOverlappingIntents()
        if (jdaIntents != jdaServiceIntents) {
            logger.warn {
                """
                    The intents given in JDAService and JDA should be the same!
                    JDA intents: ${jdaIntents.sorted()}
                    JDAService intents: ${jdaServiceIntents.sorted()}
                    Hint: you should use the factories such as create/createLight/createDefault,
                          see https://bc.freya02.dev/3.X/setup/getting-started/#creating-a-jdaservice
                """.trimIndent()
            }
        }

        val jdaCacheFlags = event.jda.cacheFlags
        val jdaServiceCacheFlags = jdaService.cacheFlags
        if (!jdaCacheFlags.containsAll(jdaServiceCacheFlags)) {
            logger.warn {
                """
                    The cache flags given in JDAService should at least be a subset of the JDA cache flags!
                    JDA cache flags: ${jdaCacheFlags.sorted()}
                    JDAService cache flags: ${jdaServiceCacheFlags.sorted()}
                    Hint: you should use the factories such as create/createLight/createDefault,
                          see https://bc.freya02.dev/3.X/setup/getting-started/#creating-a-jdaservice
                """.trimIndent()
            }
        }
    }
}

private fun Collection<GatewayIntent>.withOverlappingIntents(): Set<GatewayIntent> =
    GatewayIntent.getIntents(GatewayIntent.getRaw(this))

// Spring checks are slightly different, we want to tell the user to move them to their application environment,
// so the checks are consistent with condition annotations, as they can only check the environment
@Component
internal class SpringJDAServiceMismatchChecker {
    @BEventListener
    internal fun onJDA(event: InjectedJDAEvent, jdaConfiguration: JDAConfiguration, jdaService: JDAService) {
        val environmentIntents = jdaConfiguration.intents
        val jdaServiceIntents = jdaService.intents
        if (environmentIntents != jdaServiceIntents) {
            logger.warn {
                """
                    The intents given in JDAService and the environment should be the same!
                    Environment intents: ${environmentIntents.sorted()}
                    JDAService intents: ${jdaServiceIntents.sorted()}
                    Hint: you should get your intents from ${JDAConfiguration::intents.reference}
                """.trimIndent()
            }
        }

        val environmentCacheFlags = jdaConfiguration.cacheFlags
        val jdaServiceCacheFlags = jdaService.cacheFlags
        if (environmentCacheFlags != jdaServiceCacheFlags) {
            logger.warn {
                """
                    The cache flags given in JDAService and the environment should be the same!
                    Environment cache flags: ${environmentCacheFlags.sorted()}
                    JDAService cache flags: ${jdaServiceCacheFlags.sorted()}
                    Hint: you should get your caches flags from ${JDAConfiguration::cacheFlags.reference}
                """.trimIndent()
            }
        }
    }
}