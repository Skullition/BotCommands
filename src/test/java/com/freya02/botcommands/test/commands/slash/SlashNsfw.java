package com.freya02.botcommands.test.commands.slash;

import com.freya02.botcommands.api.commands.application.ApplicationCommand;
import com.freya02.botcommands.api.commands.application.CommandScope;
import com.freya02.botcommands.api.commands.application.annotations.NSFW;
import com.freya02.botcommands.api.commands.application.slash.GlobalSlashEvent;
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand;

public class SlashNsfw extends ApplicationCommand {
	@NSFW(dm = true)
	@JDASlashCommand(scope = CommandScope.GLOBAL, name = "nsfw")
	public void nsfw(GlobalSlashEvent event) {
		event.reply("nsfw content").setEphemeral(true).queue();
	}
}
