package com.arts256.letterbanners;

/**
 * The parts of the mod both loaders share. Everything but the entry point is
 * plain Minecraft code, so Fabric and NeoForge compile the same sources and only
 * differ in how they hand the command dispatcher over: see
 * {@code LetterBannersFabric} and {@code LetterBannersNeoForge}.
 */
public final class LetterBanners {
	public static final String MOD_ID = "letterbanners";

	private LetterBanners() {
	}
}
