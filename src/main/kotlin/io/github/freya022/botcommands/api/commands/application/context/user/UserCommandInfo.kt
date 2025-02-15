package io.github.freya022.botcommands.api.commands.application.context.user

import io.github.freya022.botcommands.api.commands.application.ApplicationCommandInfo
import io.github.freya022.botcommands.api.commands.application.TopLevelApplicationCommandInfo
import io.github.freya022.botcommands.api.commands.application.context.user.options.UserContextCommandOption
import io.github.freya022.botcommands.api.commands.application.context.user.options.UserContextCommandParameter

/**
 * Represents a context command that acts on a user.
 */
interface UserCommandInfo : TopLevelApplicationCommandInfo, ApplicationCommandInfo {
    override val topLevelInstance: UserCommandInfo

    override val parameters: List<UserContextCommandParameter>

    override val discordOptions: List<UserContextCommandOption>
        get() = parameters.flatMap { it.allOptions }.filterIsInstance<UserContextCommandOption>()

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("For removal, confusing on whether it searches nested parameters, prefer using collection operations on 'parameters' instead, make an extension or an utility method")
    override fun getParameter(declaredName: String): UserContextCommandParameter? =
        parameters.find { it.name == declaredName }
}