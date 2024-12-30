package io.github.freya022.botcommands.api.core.config

import io.github.freya022.botcommands.api.core.service.annotations.InjectedService
import io.github.freya022.botcommands.api.emojis.AppEmojisRegistry
import io.github.freya022.botcommands.internal.core.config.ConfigDSL
import io.github.freya022.botcommands.internal.core.config.ConfigurationValue

@InjectedService
interface BAppEmojisConfig {
    /**
     * Allows uploading application emojis at startup, and retrieving them from [AppEmojisRegistry].
     *
     * Default: `false`
     *
     * Spring property: `botcommands.app.emojis.enable`
     */
    @ConfigurationValue(path = "botcommands.app.emojis.enable", defaultValue = "false")
    val enable: Boolean
}

@ConfigDSL
class BAppEmojisConfigBuilder internal constructor() : BAppEmojisConfig {
    @set:JvmName("enable")
    override var enable: Boolean = false

    @JvmSynthetic
    internal fun build() = object : BAppEmojisConfig {
        override val enable: Boolean = this@BAppEmojisConfigBuilder.enable
    }
}