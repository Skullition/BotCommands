package com.freya02.botcommands.api.commands.application.slash.builder

import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.slash.builder.mixins.ITopLevelSlashCommandBuilder
import com.freya02.botcommands.api.commands.application.slash.builder.mixins.TopLevelSlashCommandBuilderMixin
import com.freya02.botcommands.internal.BContextImpl
import com.freya02.botcommands.internal.commands.application.slash.TopLevelSlashCommandInfo
import com.freya02.botcommands.internal.throwUser

class TopLevelSlashCommandBuilder internal constructor(
    context: BContextImpl,
    name: String,
    scope: CommandScope
) : SlashCommandBuilder(context, name), ITopLevelSlashCommandBuilder by TopLevelSlashCommandBuilderMixin(scope) {
    @get:JvmSynthetic
    internal val subcommands: MutableList<SlashSubcommandBuilder> = mutableListOf()
    @get:JvmSynthetic
    internal val subcommandGroups: MutableList<SlashSubcommandGroupBuilder> = mutableListOf()

    override val allowOptions: Boolean
        get() = subcommands.isEmpty() && subcommandGroups.isEmpty()
    override val allowSubcommands: Boolean
        get() = optionBuilders.isEmpty()
    override val allowSubcommandGroups: Boolean
        get() = optionBuilders.isEmpty()

    fun subcommand(name: String, block: SlashSubcommandBuilder.() -> Unit) {
        if (!allowSubcommands) throwUser("Cannot add subcommands as this already contains options")

        subcommands += SlashSubcommandBuilder(context, name).apply(block)
    }

    fun subcommandGroup(name: String, block: SlashSubcommandGroupBuilder.() -> Unit) {
        if (!allowSubcommandGroups) throwUser("Cannot add subcommand groups as this already contains options")

        subcommandGroups += SlashSubcommandGroupBuilder(context, name).apply(block)
    }

    internal fun build(): TopLevelSlashCommandInfo {
        checkFunction()
        return TopLevelSlashCommandInfo(context, this)
    }
}
