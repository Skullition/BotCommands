package com.freya02.botcommands.application.slash;

import com.freya02.botcommands.BContext;
import com.freya02.botcommands.SettingsProvider;
import com.freya02.botcommands.application.ApplicationCommandInfo;
import com.freya02.botcommands.application.ApplicationCommandParameter;
import com.freya02.botcommands.application.LocalizedCommandData;
import com.freya02.botcommands.internal.ApplicationOptionData;
import com.freya02.botcommands.internal.utils.Utils;
import com.freya02.botcommands.parameters.ParameterResolvers;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.freya02.botcommands.application.LocalizedCommandData.LocalizedOption;

public class SlashUtils {
	public static void appendCommands(List<Command> commands, StringBuilder sb) {
		for (Command command : commands) {
			final StringJoiner joiner = new StringJoiner("] [", "[", "]").setEmptyValue("");
			if (command instanceof SlashCommand) {
				for (SlashCommand.Option option : ((SlashCommand) command).getOptions()) {
					joiner.add(option.getType().name());
				}
			}

			sb.append(" - ").append(command.getName()).append(" ").append(joiner).append("\n");
		}
	}

	public static List<String> getMethodOptionNames(@NotNull ApplicationCommandInfo info) {
		final List<String> list = new ArrayList<>();

		for (ApplicationCommandParameter<?> parameter : info.getParameters()) {
			if (!parameter.isOption()) continue;

			list.add(parameter.getApplicationOptionData().getEffectiveName());
		}
		
		return list;
	}

	public static List<OptionData> getMethodOptions(@NotNull SlashCommandInfo info, @NotNull LocalizedCommandData localizedCommandData) {
		final List<OptionData> list = new ArrayList<>();
		final List<LocalizedOption> optionNames = getLocalizedOptions(info, localizedCommandData);
		final List<List<SlashCommand.Choice>> optionsChoices = getAllOptionsLocalizedChoices(localizedCommandData);

		final long optionParamCount = info.getParameters().stream().filter(ApplicationCommandParameter::isOption).count();
		Checks.check(optionNames.size() == optionParamCount, "Slash command has %s options but has %d parameters (after the event) @ %s, you should check if you return the correct number of localized strings", optionNames, optionParamCount - 1, Utils.formatMethodShort(info.getCommandMethod()));

		int i = 1;
		for (SlashCommandParameter parameter : info.getParameters()) {
			if (!parameter.isOption()) continue;

			final Class<?> type = parameter.getType();
			final ApplicationOptionData applicationOptionData = parameter.getApplicationOptionData();

			final String name = optionNames.get(i - 1).getName();
			final String description = optionNames.get(i - 1).getDescription();

			final OptionData data;
			if (type == User.class || type == Member.class) {
				data = new OptionData(OptionType.USER, name, description);
			} else if (type == Role.class) {
				data = new OptionData(OptionType.ROLE, name, description);
			} else if (type == TextChannel.class) {
				data = new OptionData(OptionType.CHANNEL, name, description);
			} else if (type == IMentionable.class) {
				data = new OptionData(OptionType.MENTIONABLE, name, description);
			} else if (type == boolean.class) {
				data = new OptionData(OptionType.BOOLEAN, name, description);
			} else if (type == long.class) {
				data = new OptionData(OptionType.INTEGER, name, description);
			} else if (type == double.class) {
				data = new OptionData(OptionType.NUMBER, name, description);
			} else if (ParameterResolvers.exists(type)) {
				data = new OptionData(OptionType.STRING, name, description);
			} else {
				throw new IllegalArgumentException("Unknown slash command option: " + type.getName());
			}

			if (data.getType().canSupportChoices()) {
				//choices might just be empty
				if (optionsChoices.size() >= i) {
					data.addChoices(optionsChoices.get(i - 1));
				}
			}

			list.add(data);

			data.setRequired(!applicationOptionData.isOptional());

			i++;
		}

		return list;
	}

	@Nonnull
	public static List<List<SlashCommand.Choice>> getNotLocalizedChoices(BContext context, @Nullable Guild guild, ApplicationCommandInfo info) {
		List<List<SlashCommand.Choice>> optionsChoices = new ArrayList<>();

		final int count = (int) info.getParameters().stream().filter(ApplicationCommandParameter::isOption).count();
		for (int optionIndex = 0; optionIndex < count; optionIndex++) {
			optionsChoices.add(getNotLocalizedChoicesForCommand(context, guild, info, optionIndex));
		}

		return optionsChoices;
	}

	@Nonnull
	private static List<SlashCommand.Choice> getNotLocalizedChoicesForCommand(BContext context, @Nullable Guild guild, ApplicationCommandInfo info, int optionIndex) {
		final List<SlashCommand.Choice> choices = info.getInstance().getOptionChoices(guild, info.getPath(), optionIndex);

		if (choices.isEmpty()) {
			final SettingsProvider settingsProvider = context.getSettingsProvider();

			if (settingsProvider != null) {
				return settingsProvider.getOptionChoices(guild, info.getPath(), optionIndex);
			}
		}

		return choices;
	}

	@Nonnull
	private static List<LocalizedOption> getLocalizedOptions(@Nonnull SlashCommandInfo info, @Nullable LocalizedCommandData localizedCommandData) {
		return localizedCommandData == null
				? getNotLocalizedMethodOptions(info)
				: Objects.requireNonNullElseGet(localizedCommandData.getLocalizedOptionNames(), () -> getNotLocalizedMethodOptions(info));
	}

	@Nonnull
	private static List<LocalizedOption> getNotLocalizedMethodOptions(@Nonnull SlashCommandInfo info) {
		return info.getParameters()
				.stream()
				.filter(ApplicationCommandParameter::isOption)
				.map(ApplicationCommandParameter::getApplicationOptionData)
				.map(param -> new LocalizedOption(param.getEffectiveName(), param.getEffectiveDescription()))
				.collect(Collectors.toList());
	}

	@Nonnull
	private static List<List<SlashCommand.Choice>> getAllOptionsLocalizedChoices(@Nullable LocalizedCommandData localizedCommandData) {
		return localizedCommandData == null
				? Collections.emptyList() //Here choices are only obtainable via the localized data as the annotations were removed.
				: Objects.requireNonNullElseGet(localizedCommandData.getLocalizedOptionChoices(), Collections::emptyList);
	}
}
