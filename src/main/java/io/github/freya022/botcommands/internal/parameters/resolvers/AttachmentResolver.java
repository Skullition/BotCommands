package io.github.freya022.botcommands.internal.parameters.resolvers;

import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver;
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Resolver
public class AttachmentResolver
        extends ClassParameterResolver<AttachmentResolver, Attachment>
        implements SlashParameterResolver<AttachmentResolver, Attachment> {

    public AttachmentResolver() {
        super(Attachment.class);
    }

    @Override
    @NotNull
    public OptionType getOptionType() {
        return OptionType.ATTACHMENT;
    }

    @Nullable
    @Override
    public Attachment resolve(@NotNull SlashCommandOption option, @NotNull CommandInteractionPayload event, @NotNull OptionMapping optionMapping) {
        return optionMapping.getAsAttachment();
    }
}
