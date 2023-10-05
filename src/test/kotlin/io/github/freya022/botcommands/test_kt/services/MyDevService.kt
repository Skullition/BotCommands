package io.github.freya022.botcommands.test_kt.services

import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.test_kt.services.annotations.RequireProfile
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

@BService
@RequireProfile(Profile.DEV)
class MyDevService {
    init {
        logger.trace { "Enabled dev service" }
    }
}