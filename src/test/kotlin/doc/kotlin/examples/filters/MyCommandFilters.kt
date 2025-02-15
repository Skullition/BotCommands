package doc.kotlin.examples.filters

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.application.ApplicationCommandFilter
import io.github.freya022.botcommands.api.commands.application.ApplicationCommandInfo
import io.github.freya022.botcommands.api.commands.application.ApplicationCommandRejectionHandler
import io.github.freya022.botcommands.api.commands.text.TextCommandFilter
import io.github.freya022.botcommands.api.commands.text.TextCommandRejectionHandler
import io.github.freya022.botcommands.api.commands.text.TextCommandVariation
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.test.switches.TestLanguage
import io.github.freya022.botcommands.test.switches.TestService
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

@BService
@TestService
@TestLanguage(TestLanguage.Language.KOTLIN)
class MyCommandFilters : TextCommandFilter<String>, ApplicationCommandFilter<String> {
    override suspend fun checkSuspend(
        event: MessageReceivedEvent,
        commandVariation: TextCommandVariation,
        args: String
    ): String? = check(event.guildChannel)

    override suspend fun checkSuspend(
        event: GenericCommandInteractionEvent,
        commandInfo: ApplicationCommandInfo
    ): String? = check(event.messageChannel)

    private fun check(channel: Channel): String? {
        if (channel.idLong != 722891685755093076) {
            return "Can only run commands in <#722891685755093076>"
        }
        return null
    }
}

@BService
@TestService
@TestLanguage(TestLanguage.Language.KOTLIN)
class MyCommandRejectionHandler : TextCommandRejectionHandler<String>, ApplicationCommandRejectionHandler<String> {
    override suspend fun handleSuspend(
        event: MessageReceivedEvent,
        variation: TextCommandVariation,
        args: String,
        userData: String
    ) {
        event.message.reply(userData).await()
    }

    override suspend fun handleSuspend(
        event: GenericCommandInteractionEvent,
        commandInfo: ApplicationCommandInfo,
        userData: String
    ) {
        event.reply_(userData, ephemeral = true).await()
    }
}