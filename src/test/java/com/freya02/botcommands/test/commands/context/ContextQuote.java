package com.freya02.botcommands.test.commands.context;

import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.context.message.GuildMessageEvent;
import com.freya02.botcommands.api.commands.application.annotations.AppOption;
import com.freya02.botcommands.api.commands.application.context.annotations.JDAMessageCommand;
import net.dv8tion.jda.api.entities.Message;

public class ContextQuote extends ApplicationCommand {
	@JDAMessageCommand(name = "Quote message")
	public void quote(GuildMessageEvent event, @AppOption Message target) {
		event.reply("Quote: " + target.getContentRaw()).queue();
	}
}
