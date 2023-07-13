package com.freya02.botcommands.api.commands.application;

import com.freya02.botcommands.api.commands.application.annotations.AppDeclaration;
import com.freya02.botcommands.api.commands.application.context.annotations.JDAMessageCommand;
import com.freya02.botcommands.api.commands.application.context.annotations.JDAUserCommand;
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand;

/**
 * Base class for <b>annotated</b> application commands (slash / context commands).
 *
 * <p>You are not required to use this if you use the {@link AppDeclaration DSL declaration mode}
 *
 * @see JDASlashCommand
 * @see JDAMessageCommand
 * @see JDAUserCommand
 */
public abstract class ApplicationCommand implements GuildApplicationSettings {}