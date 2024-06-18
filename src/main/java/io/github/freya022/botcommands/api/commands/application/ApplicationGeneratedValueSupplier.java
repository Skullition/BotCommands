package io.github.freya022.botcommands.api.commands.application;

import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ApplicationGeneratedValueSupplier {
    @Nullable
    Object getDefaultValue(@NotNull CommandInteractionPayload event);
}
