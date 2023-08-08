package com.freya02.botcommands.api.localization.arguments.factories

import com.freya02.botcommands.api.core.config.BServiceConfigBuilder
import com.freya02.botcommands.api.core.service.annotations.BService
import com.freya02.botcommands.api.core.service.annotations.InterfacedService
import com.freya02.botcommands.api.localization.arguments.FormattableArgument
import java.util.*

/**
 * Factory for [formattable arguments][FormattableArgument].
 *
 * **Usage**: Register your instance as a service with [BService]
 * or [any annotation that enables your class for dependency injection][BServiceConfigBuilder.serviceAnnotations].
 *
 * @see FormattableArgument
 * @see InterfacedService @InterfacedService
 */
@InterfacedService(acceptMultiple = true)
interface FormattableArgumentFactory {
    val regex: Regex

    fun get(matchResult: MatchResult, locale: Locale): FormattableArgument
}