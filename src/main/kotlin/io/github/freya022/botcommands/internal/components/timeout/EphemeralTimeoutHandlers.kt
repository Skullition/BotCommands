package io.github.freya022.botcommands.internal.components.timeout

import io.github.freya022.botcommands.api.components.annotations.RequiresComponents
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.internal.components.EphemeralHandlers

@BService
@RequiresComponents
internal class EphemeralTimeoutHandlers : EphemeralHandlers<suspend () -> Unit>()