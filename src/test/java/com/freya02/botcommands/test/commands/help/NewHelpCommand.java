package com.freya02.botcommands.test.commands.help;

import com.freya02.botcommands.api.commands.prefixed.BaseCommandEvent;
import com.freya02.botcommands.api.commands.prefixed.IHelpCommand;
import com.freya02.botcommands.api.commands.prefixed.TextCommand;
import com.freya02.botcommands.api.commands.prefixed.annotations.JDATextCommand;
import com.freya02.botcommands.internal.commands.prefixed.TextCommandInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class NewHelpCommand extends TextCommand implements IHelpCommand {
	@JDATextCommand(
			name = "help",
			description = "Sends help"
	)
	public void onTextHelp(BaseCommandEvent event) {
		event.reactSuccess().queue();
	}

	@Override
	public void onInvalidCommand(@NotNull BaseCommandEvent event, @NotNull Collection<TextCommandInfo> commandInfos) {
		event.reactError().queue();
	}
}
