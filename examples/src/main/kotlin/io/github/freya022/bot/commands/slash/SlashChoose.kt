package io.github.freya022.bot.commands.slash

import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.annotations.AppOption
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.commands.application.slash.annotations.VarArgs
import com.freya02.botcommands.api.core.service.annotations.BService
import com.freya02.botcommands.api.core.service.annotations.ConditionalService
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.bot.commands.FrontendChooser
import io.github.freya022.bot.commands.SimpleFrontend

@BService
class SlashChoose {
    fun onSlashChoose(event: GuildSlashEvent, choices: List<String>) {
        event.reply_(choices.random(), ephemeral = true).queue()
    }
}

@Command
@ConditionalService(FrontendChooser::class)
class SlashChooseDetailedFront {
    @AppDeclaration
    fun onDeclare(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("choose", function = SlashChoose::onSlashChoose) {
            description = "Randomly choose a value"

            optionVararg(
                declaredName = "choices",
                amount = 10,
                requiredAmount = 2,
                optionNameSupplier = { count -> "choice_$count" }
            ) { count ->
                description = "Choice N°$count"
            }
        }
    }
}

@Command
@SimpleFrontend
@ConditionalService(FrontendChooser::class)
class SlashChooseSimplifiedFront(private val slashChoose: SlashChoose) : ApplicationCommand() {
    @JDASlashCommand(name = "choose", description = "Randomly choose a value")
    fun onSlashBan(
        event: GuildSlashEvent,
        // Notice here how you are limited to 1 description for all your options
        @AppOption(name = "choice", description = "A choice") @VarArgs(10, numRequired = 2) choices: List<String>
    ) = slashChoose.onSlashChoose(event, choices)
}