package com.freya02.botcommands.api.new_components.builder

import com.freya02.botcommands.api.components.InteractionConstraints
import com.freya02.botcommands.internal.new_components.ComponentHandler
import com.freya02.botcommands.internal.new_components.ComponentType
import com.freya02.botcommands.internal.new_components.new.ComponentController
import com.freya02.botcommands.internal.new_components.new.ComponentTimeout
import com.freya02.botcommands.internal.throwUser
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

@Suppress("UNCHECKED_CAST")
internal abstract class ButtonBuilder<T : ButtonBuilder<T>>(
    private val componentController: ComponentController,
    private val style: ButtonStyle
) : ComponentBuilder {
    override val componentType: ComponentType = ComponentType.BUTTON

    final override var oneUse: Boolean = false
        protected set
    final override var constraints: InteractionConstraints = InteractionConstraints()
        protected set
    override val timeout: ComponentTimeout? = null
    override val handler: ComponentHandler? = null

    fun oneUse(): T = this.also { oneUse = true } as T

    fun constraints(block: InteractionConstraints.() -> Unit): T = this.also { constraints.apply(block) } as T

    fun build(label: String): Button = build(label, null)
    fun build(emoji: Emoji): Button = build(null, emoji)
    fun build(label: String?, emoji: Emoji?): Button {
        require(handler != null) {
            throwUser("A component handler needs to be set using #bindTo methods")
        }
        return Button.of(style, componentController.createComponent(this), label, emoji)
    }
}