package com.freya02.botcommands.internal.prefixed

import com.freya02.botcommands.api.Logging
import com.freya02.botcommands.api.application.CommandPath
import com.freya02.botcommands.api.application.builder.OptionBuilder.Companion.findOption
import com.freya02.botcommands.api.parameters.RegexParameterResolver
import com.freya02.botcommands.api.prefixed.CommandEvent
import com.freya02.botcommands.api.prefixed.builder.TextCommandBuilder
import com.freya02.botcommands.api.prefixed.builder.TextOptionBuilder
import com.freya02.botcommands.internal.*
import com.freya02.botcommands.internal.parameters.CustomMethodParameter
import com.freya02.botcommands.internal.parameters.MethodParameterType
import com.freya02.botcommands.internal.utils.ReflectionUtilsKt.nonInstanceParameters
import com.freya02.botcommands.internal.utils.Utils
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

private val LOGGER = Logging.getLogger()

class TextCommandInfo(
    private val context: BContextImpl,
    builder: TextCommandBuilder
) : AbstractCommandInfo(context, builder) {
    override val parameters: MethodParameters

    val isOwnerRequired: Boolean
    val aliases: List<CommandPath>
    val description: String
    val hidden: Boolean
    val completePattern: Pattern?
    val order: Int

    private val useTokenizedEvent: Boolean

    init {
        isOwnerRequired = builder.ownerRequired
        aliases = builder.aliases
        description = builder.description
        order = builder.order
        hidden = builder.hidden

        useTokenizedEvent = method.valueParameters.first().type.jvmErasure.isSubclassOf(CommandEvent::class)

        @Suppress("RemoveExplicitTypeArguments")
        parameters = MethodParameters2.transform<RegexParameterResolver>(
            context,
            method,
            builder.optionBuilders
        ) {
            optionPredicate = { builder.optionBuilders[it.findDeclarationName()] is TextOptionBuilder }
            optionTransformer = { parameter, paramName, resolver -> TextCommandParameter(parameter, builder.optionBuilders.findOption(paramName), resolver) }
        }

        completePattern = when {
            parameters.any { it.isOption } -> CommandPattern.of(this)
            else -> null
        }
    }

    suspend fun execute(
        _event: MessageReceivedEvent,
        args: String,
        matcher: Matcher?
    ): ExecutionResult {
        val event = when {
            useTokenizedEvent -> CommandEventImpl(context, method, _event, args)
            else -> BaseCommandEventImpl(context, method, _event, args)
        }

        val objects: MutableMap<KParameter, Any?> = hashMapOf()
        objects[method.instanceParameter!!] = instance
        objects[method.nonInstanceParameters.first()] = event

        var groupIndex = 1
        for (parameter in parameters) {
            objects[parameter.kParameter] = when (parameter.methodParameterType) {
                MethodParameterType.COMMAND -> {
                    matcher ?: throwInternal("No matcher passed for a regex command")

                    parameter as TextCommandParameter

                    var found = 0
                    val groupCount = parameter.groupCount
                    val groups = arrayOfNulls<String>(groupCount)
                    for (j in 0 until groupCount) {
                        groups[j] = matcher.group(groupIndex++)
                        if (groups[j] != null) found++
                    }

                    if (found == groupCount) { //Found all the groups
                        val resolved = parameter.resolver.resolve(context, this, event, groups)
                        //Regex matched but could not be resolved
                        // if optional then it's ok
                        if (resolved == null && !parameter.isOptional) {
                            return ExecutionResult.CONTINUE
                        }

                        resolved
                    } else if (!parameter.isOptional) { //Parameter is not found yet the pattern matched and is not optional
                        LOGGER.warn(
                            "Could not find parameter #{} in {} for input args {}",
                            parameter.index,
                            Utils.formatMethodShort(method),
                            args
                        )

                        return ExecutionResult.CONTINUE
                    } else { //Parameter is optional
                        when {
                            parameter.isPrimitive -> 0
                            else -> null
                        }
                    }
                }
                MethodParameterType.CUSTOM -> {
                    parameter as CustomMethodParameter

                    parameter.resolver.resolve(context, this, event)
                }
                MethodParameterType.GENERATED -> {
                    parameter as TextGeneratedMethodParameter

                    parameter.generatedValueSupplier.getDefaultValue(event)
                }
                else -> throwInternal("MethodParameterType#${parameter.methodParameterType} has not been implemented")
            }
        }

        applyCooldown(event) //TODO cooldown is applied on a per-alternative basis, it should be per command path

        method.callSuspendBy(objects)

        return ExecutionResult.OK
    }
}