package io.github.freya022.botcommands.internal.commands.application.context.user

import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.TopLevelApplicationCommandMetadata
import io.github.freya022.botcommands.api.commands.application.context.builder.UserCommandBuilder
import io.github.freya022.botcommands.api.commands.application.context.user.GlobalUserEvent
import io.github.freya022.botcommands.api.commands.application.context.user.GuildUserEvent
import io.github.freya022.botcommands.api.commands.application.context.user.UserCommandInfo
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.internal.commands.application.ApplicationCommandInfoImpl
import io.github.freya022.botcommands.internal.commands.application.ApplicationGeneratedOption
import io.github.freya022.botcommands.internal.commands.application.mixins.TopLevelApplicationCommandInfoMixin
import io.github.freya022.botcommands.internal.commands.application.slash.SlashUtils.getCheckedDefaultValue
import io.github.freya022.botcommands.internal.core.options.OptionImpl
import io.github.freya022.botcommands.internal.core.options.OptionType
import io.github.freya022.botcommands.internal.core.reflection.toMemberParamFunction
import io.github.freya022.botcommands.internal.parameters.CustomMethodOption
import io.github.freya022.botcommands.internal.parameters.ServiceMethodOption
import io.github.freya022.botcommands.internal.transform
import io.github.freya022.botcommands.internal.utils.*
import kotlin.reflect.full.callSuspendBy

internal class UserCommandInfoImpl internal constructor(
    override val context: BContext,
    builder: UserCommandBuilder
) : ApplicationCommandInfoImpl(builder),
    UserCommandInfo,
    TopLevelApplicationCommandInfoMixin {

    override val eventFunction = builder.toMemberParamFunction<GlobalUserEvent, _>(context)

    override val topLevelInstance get() = this
    override val parentInstance get() = null

    override val scope: CommandScope = builder.scope
    override val isDefaultLocked: Boolean = builder.isDefaultLocked
    override val isGuildOnly: Boolean = scope.isGuildOnly
    override val nsfw: Boolean = builder.nsfw

    override lateinit var metadata: TopLevelApplicationCommandMetadata

    override val parameters: List<UserContextCommandParameterImpl>

    init {
        eventFunction.checkEventScope<GuildUserEvent>(builder)

        initChecks(builder)

        parameters = builder.optionAggregateBuilders.transform {
            UserContextCommandParameterImpl(context, this, builder, it)
        }
    }

    internal suspend fun execute(event: GlobalUserEvent): Boolean {
        val optionValues = parameters.mapOptions { option ->
            if (tryInsertOption(event, this, option) == InsertOptionResult.ABORT)
                return false
        }

        val finalParameters = parameters.mapFinalParameters(event, optionValues)
        function.callSuspendBy(finalParameters)

        return true
    }

    private suspend fun tryInsertOption(
        event: GlobalUserEvent,
        optionMap: MutableMap<OptionImpl, Any?>,
        option: OptionImpl
    ): InsertOptionResult {
        val value = when (option.optionType) {
            OptionType.OPTION -> {
                option as UserContextCommandOptionImpl

                option.resolver.resolveSuspend(this, event)
            }
            OptionType.CUSTOM -> {
                option as CustomMethodOption

                option.resolver.resolveSuspend(this, event)
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
}