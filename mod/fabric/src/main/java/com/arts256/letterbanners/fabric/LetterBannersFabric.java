package com.arts256.letterbanners.fabric;

import com.arts256.letterbanners.LetterBannerCommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Fabric entry point. Wraps the Letter Banners data pack, which ships inside this
 * jar, and adds the {@code /letterbanner} command on top of it.
 */
public final class LetterBannersFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> LetterBannerCommand.register(dispatcher));
	}
}
