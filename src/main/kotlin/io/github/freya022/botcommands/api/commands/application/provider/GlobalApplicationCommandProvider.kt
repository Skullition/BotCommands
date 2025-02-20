package io.github.freya022.botcommands.api.commands.application.provider

import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import io.github.freya022.botcommands.api.commands.application.context.annotations.JDAUserCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.service.annotations.InterfacedService

/**
 * Interface to declare global application commands.
 *
 * **Note:** The function may be called more than once, for example,
 * if the bot needs to update its commands.
 *
 * **Usage**: Register your instance as a service with [@BService][BService].
 *
 * @see Command @Command
 * @see JDASlashCommand @JDASlashCommand
 * @see JDAMessageCommand @JDAMessageCommand
 * @see JDAUserCommand @JDAUserCommand
 *
 * @see InterfacedService @InterfacedService
 */
@InterfacedService(acceptMultiple = true)
interface GlobalApplicationCommandProvider {
    fun declareGlobalApplicationCommands(manager: GlobalApplicationCommandManager)
}