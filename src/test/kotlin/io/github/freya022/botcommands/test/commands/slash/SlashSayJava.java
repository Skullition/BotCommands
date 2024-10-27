package io.github.freya022.botcommands.test.commands.slash;

import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.components.Button;
import io.github.freya022.botcommands.api.components.Buttons;
import io.github.freya022.botcommands.api.components.annotations.RequiresComponents;
import io.github.freya022.botcommands.api.utils.EmojiUtils;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.time.Duration;

@Command
@RequiresComponents // Disables the command if components are not enabled
public class SlashSayJava extends ApplicationCommand {
    // Little trick to get the emoji lazily, this will reduce the startup impact
    static class Emojis {
        private static final UnicodeEmoji WASTEBASKET = EmojiUtils.resolveJDAEmoji("wastebasket");
    }

    private final Buttons buttons;

    public SlashSayJava(Buttons buttons) {
        this.buttons = buttons;
    }

    @JDASlashCommand(name = "say_java", description = "Sends a message in a channel")
    public void onSlashSay(
            GuildSlashEvent event,
            @SlashOption(description = "Channel to send the message in") TextChannel channel,
            @SlashOption(description = "What to say") String content
    ) {
        final Button deleteButton = buttons.danger(Emojis.WASTEBASKET).ephemeral()
                .bindTo(buttonEvent -> {
                    buttonEvent.deferEdit().queue();
                    buttonEvent.getHook().deleteOriginal().queue();
                })
                .build();

        event.reply("Done!")
                .setEphemeral(true)
                .delay(Duration.ofSeconds(5))
                .flatMap(InteractionHook::deleteOriginal)
                .queue();

        channel.sendMessage(content)
                .addActionRow(deleteButton)
                .queue();
    }
}
