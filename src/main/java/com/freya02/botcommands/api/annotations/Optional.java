package com.freya02.botcommands.api.annotations;

import com.freya02.botcommands.api.application.slash.annotations.JdaSlashCommand;
import com.freya02.botcommands.api.prefixed.annotations.MethodOrder;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes an optional parameter in an {@linkplain MethodOrder} or a {@linkplain JdaSlashCommand} command
 * <p>You can also use the {@link Nullable @Nullable} annotation to represent an optional parameter while benefiting from static analysis
 *
 * <h2>For regex commands: Consider this annotation as experimental</h2>
 * <p>
 * <b>The behavior of {@linkplain MethodOrder} commands is pretty unsafe if you combine strings and numbers in the same command</b>
 * </p>
 *
 * <h2>For slash commands:</h2>
 * <p>
 * <b>This works perfectly as it's just a hint for discord</b>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Optional { }