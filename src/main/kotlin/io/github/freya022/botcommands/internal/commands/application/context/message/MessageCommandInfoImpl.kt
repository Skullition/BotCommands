package io.github.freya022.botcommands.internal.commands.application.context.message

import io.github.freya022.botcommands.api.commands.application.TopLevelApplicationCommandMetadata
import io.github.freya022.botcommands.api.commands.application.context.message.GlobalMessageEvent
import io.github.freya022.botcommands.api.commands.application.context.message.GuildMessageEvent
import io.github.freya022.botcommands.api.commands.application.context.message.MessageCommandInfo
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.utils.toImmutableSet
import io.github.freya022.botcommands.internal.commands.application.ApplicationCommandInfoImpl
import io.github.freya022.botcommands.internal.commands.application.context.message.builder.MessageCommandBuilderImpl
import io.github.freya022.botcommands.internal.commands.application.context.message.options.MessageContextCommandOptionImpl
import io.github.freya022.botcommands.internal.commands.application.context.message.options.MessageContextCommandParameterImpl
import io.github.freya022.botcommands.internal.commands.application.context.message.options.builder.MessageCommandOptionAggregateBuilderImpl
import io.github.freya022.botcommands.internal.commands.application.mixins.TopLevelApplicationCommandInfoMixin
import io.github.freya022.botcommands.internal.commands.application.options.ApplicationGeneratedOption
import io.github.freya022.botcommands.internal.commands.application.slash.SlashUtils.getCheckedDefaultValue
import io.github.freya022.botcommands.internal.commands.text.TextUtils.getSpacedPath
import io.github.freya022.botcommands.internal.core.options.OptionImpl
import io.github.freya022.botcommands.internal.core.options.OptionType
import io.github.freya022.botcommands.internal.core.reflection.toMemberParamFunction
import io.github.freya022.botcommands.internal.options.transform
import io.github.freya022.botcommands.internal.parameters.CustomMethodOption
import io.github.freya022.botcommands.internal.parameters.ServiceMethodOption
import io.github.freya022.botcommands.internal.utils.*
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.Command
import kotlin.reflect.full.callSuspendBy

internal class MessageCommandInfoImpl internal constructor(
    override val context: BContext,
    builder: MessageCommandBuilderImpl
) : ApplicationCommandInfoImpl(builder),
    MessageCommandInfo,
    TopLevelApplicationCommandInfoMixin {

    override val eventFunction = builder.toMemberParamFunction<GlobalMessageEvent, _>(context)

    override val topLevelInstance get() = this
    override val parentInstance get() = null

    override val type: Command.Type get() = Command.Type.MESSAGE

    override val contexts: Set<InteractionContextType> = builder.contexts.toImmutableSet()
    override val integrationTypes: Set<IntegrationType> = builder.integrationTypes.toImmutableSet()
    override val isDefaultLocked: Boolean = builder.isDefaultLocked
    override val nsfw: Boolean = builder.nsfw

    override lateinit var metadata: TopLevelApplicationCommandMetadata

    override val parameters: List<MessageContextCommandParameterImpl>

    init {
        eventFunction.checkEventScope<GuildMessageEvent>(builder)

        initChecks(builder)

        parameters = builder.optionAggregateBuilders.transform {
            MessageContextCommandParameterImpl(context, this, builder, it as MessageCommandOptionAggregateBuilderImpl)
        }
    }

    internal suspend fun execute(event: GlobalMessageEvent): Boolean {
        val optionValues = parameters.mapOptions { option ->
            if (tryInsertOption(event, this, option) == InsertOptionResult.ABORT)
                return false
        }

        val finalParameters = parameters.mapFinalParameters(event, optionValues)
        function.callSuspendBy(finalParameters)

        return true
    }

    private suspend fun tryInsertOption(
        event: GlobalMessageEvent,
        optionMap: MutableMap<OptionImpl, Any?>,
        option: OptionImpl
    ): InsertOptionResult {
        val value = when (option.optionType) {
            OptionType.OPTION -> {
                option as MessageContextCommandOptionImpl

                option.resolver.resolveSuspend(option, event)
            }
            OptionType.CUSTOM -> {
                option as CustomMethodOption

                option.resolver.resolveSuspend(option, event)
            }
            OptionType.GENERATED -> {
                option as ApplicationGeneratedOption

                option.getCheckedDefaultValue { it.generatedValueSupplier.getDefaultValue(event) }
            }
            OptionType.SERVICE -> (option as ServiceMethodOption).getService()
            OptionType.CONSTANT -> throwInternal("${option.optionType} has not been implemented")
        }

        return tryInsertNullableOption(value, option, optionMap)
    }

    override fun toString(): String {
        return "Message context '${path.getSpacedPath()}' @ ${function.shortSignature}"
    }
}