package com.freya02.botcommands.api.components.builder.select.persistent

import com.freya02.botcommands.api.components.EntitySelectMenu
import com.freya02.botcommands.api.components.builder.*
import com.freya02.botcommands.internal.components.ComponentType
import com.freya02.botcommands.internal.components.LifetimeType
import com.freya02.botcommands.internal.components.builder.ConstrainableComponentImpl
import com.freya02.botcommands.internal.components.builder.PersistentActionableComponentImpl
import com.freya02.botcommands.internal.components.builder.PersistentTimeoutableComponentImpl
import com.freya02.botcommands.internal.components.builder.UniqueComponentImpl
import com.freya02.botcommands.internal.components.controller.ComponentController
import com.freya02.botcommands.internal.utils.throwUser
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu as JDAEntitySelectMenu

class PersistentEntitySelectBuilder internal constructor(private val componentController: ComponentController, targets: List<JDAEntitySelectMenu.SelectTarget>) :
    JDAEntitySelectMenu.Builder(""),
    IConstrainableComponent by ConstrainableComponentImpl(),
    IUniqueComponent by UniqueComponentImpl(),
    BaseComponentBuilder,
    IPersistentActionableComponent by PersistentActionableComponentImpl(),
    IPersistentTimeoutableComponent by PersistentTimeoutableComponentImpl() {
    override val componentType: ComponentType = ComponentType.SELECT_MENU
    override val lifetimeType: LifetimeType = LifetimeType.PERSISTENT

    init {
        setEntityTypes(targets)
    }

    @Deprecated("Cannot get an ID on components managed by the framework", level = DeprecationLevel.ERROR)
    override fun getId(): Nothing {
        throwUser("Cannot set an ID on components managed by the framework")
    }

    @Deprecated("Cannot set an ID on components managed by the framework", level = DeprecationLevel.ERROR)
    override fun setId(customId: String): JDAEntitySelectMenu.Builder {
        if (customId.isEmpty()) return this //Empty ID is set by super constructor
        throwUser("Cannot set an ID on components managed by the framework")
    }

    @Deprecated("Cannot build on components managed by the framework", level = DeprecationLevel.ERROR)
    override fun build(): Nothing {
        throwUser("Cannot build on components managed by the framework")
    }

    internal fun doBuild(): EntitySelectMenu {
        super.setId(componentController.createComponent(this))

        return EntitySelectMenu(componentController, super.build())
    }
}