package com.freya02.botcommands.test.commands.slash;

import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.components.Components;
import com.freya02.botcommands.api.components.InteractionConstraints;
import com.freya02.botcommands.api.pagination.interactive.InteractiveMenu;
import com.freya02.botcommands.api.pagination.interactive.InteractiveMenuBuilder;
import com.freya02.botcommands.api.pagination.interactive.SelectContent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.concurrent.TimeUnit;

public class SlashInteractiveMenu extends ApplicationCommand {
	@JDASlashCommand(name = "interactive")
	public void interactiveMenu(GuildSlashEvent event, Components componentss) {
		final InteractiveMenu menu = new InteractiveMenuBuilder()
				.addMenu(SelectContent.of("Joy", "This sparks joy", Emoji.fromUnicode("\uD83D\uDE02")), 3, (interactiveMenu, pageNumber, messageBuilder, components) -> {
					components.addComponents(
							componentss.dangerButton(buttonEvent -> {
								event.getHook().deleteOriginal().queue();

								interactiveMenu.cancelTimeout();

								interactiveMenu.cleanup(event.getContext());
							}).build("Delete"),

							componentss.secondaryButton(buttonEvent -> {
								interactiveMenu.setSelectedItem("Grin");

								buttonEvent.editMessage(interactiveMenu.get()).queue();
							}).build("Go to 'Grin'"));

					return new EmbedBuilder().setTitle("This sparks joy").setDescription("Page #" + pageNumber).build();
				})
				.addMenu(SelectContent.of("Grin", "This does not spark joy", Emoji.fromUnicode("\uD83D\uDE00")), 3, (interactiveMenu, pageNumber, messageBuilder, components) -> {
					components.addComponents(
							componentss.dangerButton(buttonEvent -> {
								event.getHook().deleteOriginal().queue();

								interactiveMenu.cancelTimeout();

								interactiveMenu.cleanup(event.getContext());
							}).build("Delete"),

							componentss.secondaryButton(buttonEvent -> {
								interactiveMenu.setSelectedItem(0);

								buttonEvent.editMessage(interactiveMenu.get()).queue();
							}).build("Go to 'Joy'"));

					return new EmbedBuilder().setTitle("This does not spark joy").setDescription("Page #" + pageNumber).build();
				})
				.setConstraints(InteractionConstraints.ofUsers(event.getUser()))
				.setTimeout(5, TimeUnit.SECONDS, (interactiveMenu, msg) -> {
					System.out.println("bru");

					interactiveMenu.cleanup(event.getContext());
				})
				.build();

		event.reply(menu.get()).setEphemeral(false).queue();
	}
}
