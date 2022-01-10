package com.freya02.botcommands.api.builder;

import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.application.ApplicationCommandFilter;
import com.freya02.botcommands.api.application.annotations.Test;
import com.freya02.botcommands.internal.BContextImpl;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ApplicationCommandsBuilder {
	private final BContextImpl context;

	private final List<Long> slashGuildIds = new ArrayList<>();

	public ApplicationCommandsBuilder(BContextImpl context) {
		this.context = context;
	}

	public List<Long> getSlashGuildIds() {
		return slashGuildIds;
	}

	/**
	 * Add an application command filter for this context
	 *
	 * @param commandFilter The application command filter to add
	 * @return This builder for chaining convenience
	 * @see BContext#addApplicationFilter(ApplicationCommandFilter)
	 */
	@NotNull
	public ApplicationCommandsBuilder addApplicationFilter(@NotNull ApplicationCommandFilter commandFilter) {
		context.addApplicationFilter(commandFilter);

		return this;
	}

	/**
	 * Debug feature - Makes it so application commands are only updated on these guilds
	 *
	 * @param slashGuildIds IDs of the guilds
	 * @return This builder for chaining convenience
	 */
	public ApplicationCommandsBuilder updateCommandsOnGuildIds(List<Long> slashGuildIds) {
		this.slashGuildIds.clear();
		this.slashGuildIds.addAll(slashGuildIds);

		return this;
	}

	/**
	 * Adds test guilds IDs for all commands annotated with {@link Test}
	 *
	 * @param guildIds The test {@link Guild} IDs
	 * @return This builder for chaining convenience
	 * @see BContext#getTestGuildIds()
	 * @see Test
	 */
	public ApplicationCommandsBuilder addTestGuilds(long... guildIds) {
		context.addTestGuildIds(guildIds);

		return this;
	}
}
