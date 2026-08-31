package com.arts256.letterbanners.neoforge;

import com.arts256.letterbanners.LetterBannerCommand;
import com.arts256.letterbanners.LetterBanners;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * NeoForge entry point. Wraps the Letter Banners data pack, which ships inside
 * this jar, and adds the {@code /letterbanner} command on top of it.
 */
@Mod(LetterBanners.MOD_ID)
public final class LetterBannersNeoForge {
	/**
	 * Commands are registered per server, so the listener goes on the game bus
	 * rather than the mod bus this constructor is handed.
	 */
	public LetterBannersNeoForge(IEventBus modBus) {
		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
				event -> LetterBannerCommand.register(event.getDispatcher()));
	}
}
