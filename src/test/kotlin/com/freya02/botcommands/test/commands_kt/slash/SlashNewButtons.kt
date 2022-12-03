package com.freya02.botcommands.test.commands_kt.slash

import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.components.annotations.JDAButtonListener
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.new_components.Button
import com.freya02.botcommands.api.new_components.ComponentTimeoutData
import com.freya02.botcommands.api.new_components.Components
import com.freya02.botcommands.api.new_components.GroupTimeoutData
import com.freya02.botcommands.api.new_components.annotations.ComponentTimeoutHandler
import com.freya02.botcommands.api.new_components.annotations.GroupTimeoutHandler
import dev.minn.jda.ktx.interactions.components.asDisabled
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import java.util.concurrent.ThreadLocalRandom
import kotlin.time.Duration.Companion.seconds

@CommandMarker
class SlashNewButtons(private val components: Components) : ApplicationCommand() {
    @JDASlashCommand(name = "new_buttons")
    suspend fun onSlashNewButtons(event: GuildSlashEvent) {
        val persistentButton = persistentGroupTest(event)
        val ephemeralButton = ephemeralGroupTest(event)

        //These *should* be able to store continuations and throw a TimeoutException once the timeout is met
//        val groupEvent: GenericComponentInteractionCreateEvent = firstGroup.await()
//        val buttonEvent: ButtonEvent = firstButton.await()

        event.reply("OK, button ID: ${persistentButton.id}")
            .addActionRow(persistentButton, ephemeralButton)
            .queue()

        try {
            withTimeout(5.seconds) {
                val buttonEvent: ButtonEvent = ephemeralButton.await()
                event.hook.send("Done awaiting !", ephemeral = true).queue()
            }
        } catch (e: TimeoutCancellationException) {
            event.hook.send("Too slow", ephemeral = true).queue()
        }
    }

    private suspend fun persistentGroupTest(event: GuildSlashEvent): Button {
        val firstButton = components.persistentButton(ButtonStyle.PRIMARY, "Persistent") {
            oneUse = true //Cancels whole group if used
            constraints {
                addUserIds(1234L)
                permissions += Permission.ADMINISTRATOR
            }
            bindTo(PERSISTENT_BUTTON_LISTENER_NAME, ThreadLocalRandom.current().nextDouble(), event.member)
        }

        val secondButton = components.persistentButton(ButtonStyle.PRIMARY, "Invisible") {
            oneUse = true //Cancels whole group if used
            constraints {
                addUserIds(1234L)
                permissions += Permission.ADMINISTRATOR
            }
            bindTo(PERSISTENT_BUTTON_LISTENER_NAME, ThreadLocalRandom.current().nextDouble(), event.member)
        }

        components.newGroup(firstButton, secondButton) {
            oneUse = true
            timeout(10.seconds, PERSISTENT_GROUP_TIMEOUT_LISTENER_NAME)
        }
        return firstButton
    }

    private suspend fun ephemeralGroupTest(event: GuildSlashEvent): Button {
        val firstButton = components.ephemeralButton(ButtonStyle.SECONDARY, "Ephemeral") {
            oneUse = true //Cancels whole group if used
            constraints {
                addUserIds(1234L)
                permissions += Permission.ADMINISTRATOR
            }
            bindTo { evt -> evt.reply_("Ephemeral button clicked", ephemeral = true).queue() }
        }

        components.newGroup(firstButton) {
            oneUse = true
            timeout(15.seconds) {
                event.hook.retrieveOriginal()
                    .flatMap { event.hook.editOriginalComponents(it.components.asDisabled()) }
                    .queue()
            }
        }
        return firstButton
    }

    @JDAButtonListener(name = PERSISTENT_BUTTON_LISTENER_NAME)
    fun onFirstButtonClicked(event: ButtonEvent) {
        event.reply_("Persistent button clicked", ephemeral = true).queue()
    }

    @ComponentTimeoutHandler(name = PERSISTENT_BUTTON_TIMEOUT_LISTENER_NAME)
    fun onFirstButtonTimeout(data: ComponentTimeoutData) {
        println(data)
    }

    @GroupTimeoutHandler(name = PERSISTENT_GROUP_TIMEOUT_LISTENER_NAME)
    fun onFirstGroupTimeout(data: GroupTimeoutData) {
        println(data)
    }

    companion object {
        private const val PERSISTENT_BUTTON_LISTENER_NAME = "SlashNewButtons: persistentButton"
        private const val PERSISTENT_BUTTON_TIMEOUT_LISTENER_NAME = "SlashNewButtons: persistentButtonTimeout"
        private const val PERSISTENT_GROUP_TIMEOUT_LISTENER_NAME = "SlashNewButtons: persistentGroupTimeout"
    }
}