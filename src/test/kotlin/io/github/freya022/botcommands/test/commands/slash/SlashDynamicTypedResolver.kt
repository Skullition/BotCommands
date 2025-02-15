package io.github.freya022.botcommands.test.commands.slash

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.internal.core.service.annotations.RequiresDefaultInjection

@Command
// As the framework checks if a custom option isn't in reality a service option,
// it tries to get the Map<String, Double> from the DI container,
// causing a NoUniqueBeanDefinitionException, which isn't (and mustn't be) caught
@RequiresDefaultInjection
class SlashDynamicTypedResolver : ApplicationCommand() {
    @JDASlashCommand(name = "dynamic_typed_resolver")
    suspend fun onSlashDynamicTypedResolver(
        event: GuildSlashEvent,
        // The point is this shouldn't work for any other type of Map, see MapResolverFactory, StringDoubleMapResolver
        map: Map<String, Double>,
        // This one gets transformed from a single NUMBER option into a singleton list
        @SlashOption list: List<Double>
    ) {
        event.reply_("""
            map: $map
            list: $list
            """.trimIndent(), ephemeral = true).await()
    }
}