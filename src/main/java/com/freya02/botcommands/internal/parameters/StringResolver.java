package com.freya02.botcommands.internal.parameters;

import com.freya02.botcommands.api.parameters.*;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.CommandPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class StringResolver extends ParameterResolver implements RegexParameterResolver, QuotableRegexParameterResolver, SlashParameterResolver, ComponentParameterResolver {
	public StringResolver() {
		super(String.class);
	}

	@Override
	@Nullable
	public Object resolve(MessageReceivedEvent event, String[] args) {
		return args[0];
	}

	@Override
	@NotNull
	public Pattern getPattern() {
		return Pattern.compile("(\\X+)");
	}

	@Override
	public Pattern getQuotedPattern() {
		return Pattern.compile("\"(\\X+)\"");
	}

	@Override
	@NotNull
	public String getTestExample() {
		return "foobar";
	}

	@Override
	@Nullable
	public Object resolve(CommandPayload event, OptionMapping optionMapping) {
		return optionMapping.getAsString();
	}

	@Override
	@Nullable
	public Object resolve(GenericComponentInteractionCreateEvent event, String arg) {
		return arg;
	}
}
