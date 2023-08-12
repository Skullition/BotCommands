package com.freya02.botcommands.test_kt.commands.slash

import com.freya02.botcommands.api.commands.annotations.BotPermissions
import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.annotations.UserPermissions
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.core.utils.enumSetOf
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.Permission

@Command
class SlashPermissions : ApplicationCommand() {
    @JDASlashCommand(scope = CommandScope.GLOBAL_NO_DM, name = "permissions_annotated")
    @BotPermissions(Permission.MANAGE_EVENTS)
    @UserPermissions(Permission.MANAGE_SERVER, Permission.ADMINISTRATOR)
    fun onSlashPermissions(event: GuildSlashEvent) {
        event.reply_("Granted", ephemeral = true).queue()
    }

    @AppDeclaration
    fun declare(manager: GlobalApplicationCommandManager) {
        manager.slashCommand("permissions", scope = CommandScope.GLOBAL_NO_DM, function = ::onSlashPermissions) {
            botPermissions += Permission.MANAGE_EVENTS
            userPermissions = enumSetOf(Permission.MANAGE_SERVER, Permission.ADMINISTRATOR)
        }
    }
}