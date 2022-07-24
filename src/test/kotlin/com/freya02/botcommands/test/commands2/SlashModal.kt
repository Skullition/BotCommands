package com.freya02.botcommands.test.commands2

import com.freya02.botcommands.annotations.api.annotations.CommandMarker
import com.freya02.botcommands.annotations.api.modals.annotations.ModalData
import com.freya02.botcommands.annotations.api.modals.annotations.ModalHandler
import com.freya02.botcommands.annotations.api.modals.annotations.ModalInput
import com.freya02.botcommands.api.annotations.Declaration
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.GlobalApplicationCommandManager
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.modals.Modals
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import java.util.concurrent.TimeUnit

private const val SLASH_MODAL_MODAL_HANDLER = "SlashModal: modalHandler"
private const val SLASH_MODAL_TEXT_INPUT = "SlashModal: textInput"

@CommandMarker
class SlashModal : ApplicationCommand() {
    @CommandMarker
    fun onSlashModal(event: GuildSlashEvent) {
        val input = Modals.createTextInput(SLASH_MODAL_TEXT_INPUT, "Sample text", TextInputStyle.SHORT)
            .build()

        val modal = Modals.create("Title", SLASH_MODAL_MODAL_HANDLER, "User data", 420)
            .setTimeout(30, TimeUnit.SECONDS) {
                println("Timeout")
            }
            .addActionRow(input)
            .build()

        event.replyModal(modal).queue()
    }

    @ModalHandler(name = SLASH_MODAL_MODAL_HANDLER)
    fun onModalSubmitted(
        event: ModalInteractionEvent,
        @ModalData dataStr: String,
        @ModalData dataInt: Int,
        @ModalInput(name = SLASH_MODAL_TEXT_INPUT) inputStr: String
    ) {
        event.reply_("""
            Submitted:
            dataStr: $dataStr
            dataInt: $dataInt
            inputStr: $inputStr
            """.trimIndent(), ephemeral = true).queue()
    }

    @Declaration
    fun declare(globalApplicationCommandManager: GlobalApplicationCommandManager) {
        globalApplicationCommandManager.slashCommand("modal") {
            function = ::onSlashModal
        }
    }
}