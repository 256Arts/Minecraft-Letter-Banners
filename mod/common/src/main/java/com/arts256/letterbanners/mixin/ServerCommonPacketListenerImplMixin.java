package com.arts256.letterbanners.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.arts256.letterbanners.LetterBannerDialog;

import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Vanilla logs custom click actions and drops them, and Fabric API has no hook
 * of its own, so the dialog's button is picked up here.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
abstract class ServerCommonPacketListenerImplMixin {
	/**
	 * Injected at the tail so vanilla's thread check has already bounced the
	 * packet onto the server thread, and only the in-game listener is handled
	 * because the configuration one has no player to give banners to.
	 */
	@Inject(method = "handleCustomClickAction", at = @At("TAIL"))
	private void letterbanners$handleCustomClickAction(ServerboundCustomClickActionPacket packet, CallbackInfo info) {
		if ((Object) this instanceof ServerGamePacketListenerImpl listener) {
			LetterBannerDialog.handleCustomClickAction(listener.getPlayer(), packet.id(), packet.payload());
		}
	}
}
