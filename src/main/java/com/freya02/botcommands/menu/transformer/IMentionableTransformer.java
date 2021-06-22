package com.freya02.botcommands.menu.transformer;

import net.dv8tion.jda.api.entities.IMentionable;

public class IMentionableTransformer implements EntryTransformer<IMentionable> {
	@Override
	public String toString(IMentionable mentionable) {
		return mentionable.getAsMention();
	}
}
