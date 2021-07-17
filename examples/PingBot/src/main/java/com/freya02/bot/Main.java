package com.freya02.bot;

import com.freya02.botcommands.CommandsBuilder;
import com.freya02.botcommands.Logging;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {
	private static final Logger LOGGER = Logging.getLogger();

	public static void main(String[] args) {
		try {
			//Make sure that the file Config.json exists under src/main/java/resources/com/freya02/bot/Config.json and has a valid token inside
			final Config config = Config.readConfig();

			//Set up JDA
			final JDA jda = JDABuilder.createLight(config.getToken())
					.setActivity(Activity.playing("Prefix is !"))
					.build()
					.awaitReady();

			//Print some information about the bot
			LOGGER.info("Bot connected as {}", jda.getSelfUser().getAsTag());
			LOGGER.info("The bot is present on these guilds :");
			for (Guild guild : jda.getGuildCache()) {
				LOGGER.info("\t- {} ({})", guild.getName(), guild.getId());
			}

			//Build the command framework:
			// Prefix: !
			// Owner: User with the ID 222046562543468545
			// Commands package: com.freya02.bot.commands
			CommandsBuilder.withPrefix("!", 222046562543468545L)
					.build(jda, "com.freya02.bot.commands"); //Registering listeners is taken care of by the lib
		} catch (IOException | InterruptedException | LoginException e) {
			LOGGER.error("Unable to start the bot", e);
			System.exit(-1);
		}
	}
}