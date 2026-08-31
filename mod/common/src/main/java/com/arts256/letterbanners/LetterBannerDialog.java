package com.arts256.letterbanners;

import java.util.Optional;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

import com.arts256.letterbanners.LetterBannerDialogs.State;

/**
 * Answers the buttons of the dialog {@link LetterBannerDialogs} builds.
 *
 * <p>The data pack's copy of the dialog has to run a command, which the client
 * guards with an "are you sure you want to run a command?" screen every time.
 * The mod's dialog sends custom click actions instead, which the client forwards
 * without asking, and which this class turns back into banners and into the next
 * dialog.
 */
public final class LetterBannerDialog {
	private LetterBannerDialog() {
	}

	/**
	 * Handles one click of the dialog, ignoring every other custom action on the
	 * server.
	 */
	public static void handleCustomClickAction(ServerPlayer player, Identifier id, Optional<Tag> payload) {
		// Any client can send this packet at any time, so it earns no more trust
		// than /letterbanner itself, and everything below it is read defensively.
		if (!LetterBanners.MOD_ID.equals(id.getNamespace()) || !Commands.LEVEL_GAMEMASTERS.check(player.permissions())) {
			return;
		}

		if (!(payload.orElse(null) instanceof CompoundTag fields)) {
			return;
		}

		State state = State.read(fields);
		String slot = fields.getStringOr(LetterBannerDialogs.SLOT_KEY, LetterBannerDialogs.TEXT_SLOT);

		switch (id.getPath()) {
			case "submit" -> submit(player, state);
			case "give_set" -> giveSet(player, state, fields.getStringOr(LetterBannerDialogs.SET_KEY, ""));
			case "pick_color" -> LetterBannerDialogs.openPalette(player, slot, state);
			case "set_color" -> LetterBannerDialogs.openMain(player, state.with(slot,
					DyeColor.byName(fields.getStringOr(LetterBannerDialogs.COLOR_KEY, ""), state.color(slot))));
			default -> {
			}
		}
	}

	private static void submit(ServerPlayer player, State state) {
		// One line of feedback for the whole text, the way the command reports
		// it, rather than the data pack's "Gave 1 banner" per letter.
		attempt(player, () -> LetterBannerCommand.giveAndReport(player.createCommandSourceStack(), state.text(),
				state.textColor(), state.backgroundColor(), state.size(), state.fade()));
	}

	/** A whole set at once, in whatever colors and size the dialog is set to. */
	private static void giveSet(ServerPlayer player, State state, String set) {
		attempt(player, () -> LetterBannerCommand.giveSet(player.createCommandSourceStack(), set,
				state.textColor(), state.backgroundColor(), state.size(), state.fade()));
	}

	/**
	 * Runs one of the give paths, reporting what it throws where the player can
	 * see it.
	 *
	 * <p>Getting the banners is the end of the job, so a run that gives them
	 * closes the dialog. A run that throws leaves it open instead, with the text
	 * and colors still in it, so the player can fix what they typed. The dialog
	 * itself is left on {@code after_action: "none"} and closed from here, since
	 * closing on every button would take the color buttons' palette with it.
	 */
	private static void attempt(ServerPlayer player, Give give) {
		try {
			give.run();
		} catch (CommandSyntaxException failure) {
			// The dialog is still open, so this has to reach the player as chat
			// rather than as a command error nobody would see.
			player.sendSystemMessage(ComponentUtils.fromMessage(failure.getRawMessage()).copy().withStyle(ChatFormatting.RED));
			return;
		}

		player.connection.send(ClientboundClearDialogPacket.INSTANCE);
	}

	@FunctionalInterface
	private interface Give {
		void run() throws CommandSyntaxException;
	}
}
