package com.arts256.letterbanners;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import org.slf4j.Logger;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

/**
 * Builds the mod's dialog.
 *
 * <p>The data pack's dialog picks colors with cycle buttons, because a data pack
 * dialog is a static file: it cannot know what the player has picked, so the
 * picker has to hold that state itself. This one is built per click instead, so
 * the colors become a row of dye swatches and the dialog can show which one is
 * selected. Everything the dialog needs travels in the buttons' {@code additions}
 * and comes back in the next click's payload, so no per-player state is kept
 * here.
 */
public final class LetterBannerDialogs {
	private static final Logger LOGGER = LogUtils.getLogger();

	/** Payload keys, matching the macro arguments the data pack expects. */
	static final String TEXT_SLOT = "fg";
	static final String BACKGROUND_SLOT = "bg";
	static final String SLOT_KEY = "slot";
	static final String COLOR_KEY = "color";
	static final String SET_KEY = "set";

	/**
	 * The order the data pack's dialog lists dyes in -- greys, then the color
	 * wheel. Keep in step with DYES in tools/generate.py.
	 */
	private static final DyeColor[] PALETTE = {
		DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
		DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW,
		DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE,
		DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK,
	};

	/**
	 * The whole-set buttons, which spell a set without the player typing it. The
	 * data pack generates a function per set, and the mod runs those same ones,
	 * so keep this in step with SETS in tools/generate.py.
	 */
	record CharacterSet(String id, String label, String buttonLabel, int count) {
	}

	static final List<CharacterSet> CHARACTER_SETS = List.of(
			new CharacterSet("letters", "All Letters (A-Z)", "Make A-Z", 26),
			new CharacterSet("digits", "All Digits (0-9)", "Make 0-9", 10),
			new CharacterSet("symbols", "All Symbols", "Make Symbols", 20));

	/**
	 * The fade the banner is given: one vanilla gradient layer in the background
	 * color, either solid along the top edge and fading down
	 * ({@code minecraft:gradient}) or solid along the bottom and fading up
	 * ({@code minecraft:gradient_up}). A side rather than a toggle each, because
	 * a banner carrying both is solid background along both edges with the glyph
	 * buried in between. Keep in step with FADES in tools/generate.py.
	 */
	enum Fade {
		NONE("none", "None"), TOP("top", "Top"), BOTTOM("bottom", "Bottom");

		private final String id;
		private final String label;

		Fade(String id, String label) {
			this.id = id;
			this.label = label;
		}

		String id() {
			return id;
		}

		String label() {
			return label;
		}

		/** The macro argument create.mcfunction takes. */
		String arguments() {
			return "fade:\"%s\"".formatted(id);
		}

		/** Read from a click payload, so anything unrecognized falls back to none. */
		static Fade byId(String id) {
			for (Fade fade : values()) {
				if (fade.id.equals(id)) {
					return fade;
				}
			}

			return NONE;
		}
	}

	/** A dye button is as wide as a button is tall, so it comes out square. */
	private static final int SWATCH_WIDTH = 20;

	/**
	 * One banner per character, so a long line is a lot of banners. Matches
	 * TEXT_MAX_LENGTH in tools/generate.py.
	 */
	private static final int TEXT_MAX_LENGTH = 32;

	private static final int BUTTON_WIDTH = 200;

	/**
	 * The action buttons sit in a three column grid, so they are a third of the
	 * width the inputs are. Three of these and the spacing between them come out
	 * a little over the 300 the body is wide, which is as wide as the dialog can
	 * go before it runs past the narrowest screen the game will lay out.
	 */
	private static final int GRID_BUTTON_WIDTH = 100;
	private static final int BODY_WIDTH = 300;

	/** U+25A0, the filled square the swatches are drawn with. */
	private static final String SWATCH = "■ ";

	private LetterBannerDialogs() {
	}

	/** Everything the dialog carries between clicks. */
	record State(String text, DyeColor textColor, DyeColor backgroundColor, String size, Fade fade) {
		static final State DEFAULT = new State("A", DyeColor.BLACK, DyeColor.WHITE, "large", Fade.NONE);

		/**
		 * Reads a click payload, which is the clicked button's additions plus the
		 * dialog's inputs. Every field is read defensively: any client can send
		 * this packet with anything in it.
		 */
		static State read(CompoundTag payload) {
			return new State(
					payload.getStringOr("text", DEFAULT.text()),
					DyeColor.byName(payload.getStringOr(TEXT_SLOT, ""), DEFAULT.textColor()),
					DyeColor.byName(payload.getStringOr(BACKGROUND_SLOT, ""), DEFAULT.backgroundColor()),
					"small".equals(payload.getStringOr("size", "")) ? "small" : "large",
					Fade.byId(payload.getStringOr("fade", "")));
		}

		State with(String slot, DyeColor color) {
			return BACKGROUND_SLOT.equals(slot)
					? new State(text, textColor, color, size, fade)
					: new State(text, color, backgroundColor, size, fade);
		}

		DyeColor color(String slot) {
			return BACKGROUND_SLOT.equals(slot) ? backgroundColor : textColor;
		}

		/** The two colors, which the main dialog holds in its buttons rather than in an input. */
		JsonObject colors() {
			JsonObject additions = new JsonObject();
			additions.addProperty(TEXT_SLOT, textColor.getSerializedName());
			additions.addProperty(BACKGROUND_SLOT, backgroundColor.getSerializedName());
			return additions;
		}

		/** The whole state, for the palette, which has no inputs of its own to carry it. */
		JsonObject all() {
			JsonObject additions = colors();
			additions.addProperty("text", text);
			additions.addProperty("size", size);
			additions.addProperty("fade", fade.id());
			return additions;
		}
	}

	static void openMain(ServerPlayer player, State state) {
		open(player, mainDialog(state));
	}

	static void openPalette(ServerPlayer player, String slot, State state) {
		open(player, paletteDialog(slot, state));
	}

	/**
	 * The dialog {@code /letterbanner} and {@code /function letterbanner} open:
	 * the same text, size and fade controls as the data pack's dialog,
	 * with the two color cycle buttons replaced by buttons that open a palette.
	 */
	static JsonObject mainDialog(State state) {
		JsonArray inputs = new JsonArray();

		JsonObject text = input("minecraft:text", "text", "Text");
		text.addProperty("width", BUTTON_WIDTH);
		text.addProperty("max_length", TEXT_MAX_LENGTH);
		text.addProperty("initial", state.text());
		inputs.add(text);

		JsonObject size = input("minecraft:single_option", "size", "Font Size");
		size.addProperty("width", BUTTON_WIDTH);
		JsonArray sizes = new JsonArray();
		sizes.add(option("large", literal("Large"), "large".equals(state.size())));
		sizes.add(option("small", literal("Small"), "small".equals(state.size())));
		size.add("options", sizes);
		inputs.add(size);

		JsonObject fade = input("minecraft:single_option", "fade", "Fade");
		fade.addProperty("width", BUTTON_WIDTH);
		JsonArray fades = new JsonArray();

		for (Fade option : Fade.values()) {
			fades.add(option(option.id(), literal(option.label()), option == state.fade()));
		}

		fade.add("options", fades);
		inputs.add(fade);

		// Six buttons in three columns, so the whole-set buttons make a single row
		// under the colors and Make Banners rather than a stack of full width buttons
		// as tall as the inputs above them. The client fills the grid row by row,
		// so the order here is the order they read in.
		JsonArray actions = new JsonArray();
		actions.add(paletteButton("Text: ", TEXT_SLOT, state));
		actions.add(paletteButton("Background: ", BACKGROUND_SLOT, state));

		JsonObject create = button(literal("Make Banners"), GRID_BUTTON_WIDTH, action("submit", state.colors()));
		create.add("tooltip", literal("One banner per character. The dialog closes once they are given."));
		actions.add(create);

		for (CharacterSet characters : CHARACTER_SETS) {
			JsonObject additions = state.colors();
			additions.addProperty(SET_KEY, characters.id());

			JsonObject whole = button(literal(characters.buttonLabel()), GRID_BUTTON_WIDTH,
					action("give_set", additions));
			whole.add("tooltip", literal("%s. One banner per character, %d in all, in the colors picked above."
					.formatted(characters.label(), characters.count())));
			actions.add(whole);
		}

		JsonObject dialog = dialog("Letter Banners", 3, actions);
		dialog.add("body", body(literal("Type letters (A-Z), digits (0-9), symbols and spaces, pick your colors,"
				+ " and one banner per character is added to your inventory.", "gray")));
		dialog.add("inputs", inputs);
		return dialog;
	}

	/** One dye per button, in a single row, with the current pick named above them. */
	static JsonObject paletteDialog(String slot, State state) {
		boolean background = BACKGROUND_SLOT.equals(slot);
		String title = background ? "Background Color" : "Text Color";
		DyeColor current = state.color(slot);

		JsonArray actions = new JsonArray();

		for (DyeColor color : PALETTE) {
			JsonObject additions = state.all();
			additions.addProperty(SLOT_KEY, background ? BACKGROUND_SLOT : TEXT_SLOT);
			additions.addProperty(COLOR_KEY, color.getSerializedName());

			JsonObject swatch = button(dyeSprite(color), SWATCH_WIDTH, action("set_color", additions));
			swatch.add("tooltip", swatch(color == current ? "Current: " : "", color));
			actions.add(swatch);
		}

		JsonObject dialog = dialog(title, PALETTE.length, actions);
		dialog.add("body", body(swatch("Current: ", current)));

		// Doubles as what Escape does, so backing out of the palette lands back
		// on the dialog the player came from rather than closing everything. The
		// color it sends is the one already picked, so it changes nothing.
		JsonObject additions = state.all();
		additions.addProperty(SLOT_KEY, background ? BACKGROUND_SLOT : TEXT_SLOT);
		additions.addProperty(COLOR_KEY, current.getSerializedName());
		dialog.add("exit_action", button(literal("Back"), BUTTON_WIDTH, action("set_color", additions)));
		return dialog;
	}

	private static void open(ServerPlayer player, JsonObject json) {
		build(player.registryAccess(), json).ifPresent(dialog -> player.openDialog(Holder.direct(dialog)));
	}

	/** Turns one of the builders above into a dialog that can be sent without registering it. */
	static Optional<Dialog> build(HolderLookup.Provider registries, JsonObject json) {
		RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

		return Dialog.DIRECT_CODEC.parse(ops, json)
				.resultOrPartial(error -> LOGGER.error("Letter Banners built a dialog it cannot send: {}", error));
	}

	private static JsonObject dialog(String title, int columns, JsonArray actions) {
		JsonObject dialog = new JsonObject();
		dialog.addProperty("type", "minecraft:multi_action");
		dialog.add("title", literal(title));
		dialog.add("external_title", literal("Letter Banners"));
		dialog.addProperty("can_close_with_escape", true);
		dialog.addProperty("pause", false);
		dialog.addProperty("after_action", "none");
		dialog.addProperty("columns", columns);
		dialog.add("actions", actions);
		return dialog;
	}

	private static JsonArray body(JsonObject message) {
		JsonObject entry = new JsonObject();
		entry.addProperty("type", "minecraft:plain_message");
		entry.add("contents", message);
		entry.addProperty("width", BODY_WIDTH);

		JsonArray body = new JsonArray();
		body.add(entry);
		return body;
	}

	private static JsonObject input(String type, String key, String label) {
		JsonObject input = new JsonObject();
		input.addProperty("type", type);
		input.addProperty("key", key);
		input.add("label", literal(label));
		return input;
	}

	private static JsonObject option(String id, JsonObject display, boolean initial) {
		JsonObject option = new JsonObject();
		option.addProperty("id", id);
		option.add("display", display);

		if (initial) {
			option.addProperty("initial", true);
		}

		return option;
	}

	private static JsonObject paletteButton(String label, String slot, State state) {
		JsonObject additions = state.colors();
		additions.addProperty(SLOT_KEY, slot);
		JsonObject picker = button(swatch(label, state.color(slot)), GRID_BUTTON_WIDTH,
				action("pick_color", additions));
		picker.add("tooltip", literal(BACKGROUND_SLOT.equals(slot) ? "Background Color" : "Text Color"));
		return picker;
	}

	private static JsonObject button(JsonObject label, int width, JsonObject action) {
		JsonObject button = new JsonObject();
		button.add("label", label);
		button.addProperty("width", width);
		button.add("action", action);
		return button;
	}

	private static JsonObject action(String id, JsonObject additions) {
		JsonObject action = new JsonObject();
		action.addProperty("type", "minecraft:dynamic/custom");
		action.addProperty("id", LetterBanners.MOD_ID + ":" + id);
		action.add("additions", additions);
		return action;
	}

	/**
	 * The dye's item texture, drawn straight from the atlas item models use.
	 *
	 * <p>The atlas has to be named: it defaults to the block atlas, which item
	 * textures are not stitched into, so leaving it out draws the missing
	 * texture.
	 */
	private static JsonObject dyeSprite(DyeColor color) {
		JsonObject sprite = new JsonObject();
		sprite.addProperty("type", "object");
		sprite.addProperty("object", "atlas");
		sprite.addProperty("atlas", "minecraft:items");
		sprite.addProperty("sprite", "minecraft:item/" + color.getSerializedName() + "_dye");
		// Shown instead if the texture is missing, so a resource pack cannot
		// leave the row blank.
		sprite.add("fallback", swatch("", color));
		return sprite;
	}

	/**
	 * A square of the dye, after {@code prefix} and before the dye's name.
	 *
	 * <p>The square sits in "extra" rather than first in a list so that it is the
	 * only part carrying a color: siblings inherit from their parent, so the
	 * prefix and the name keep whatever color they are drawn in.
	 */
	private static JsonObject swatch(String prefix, DyeColor color) {
		JsonObject square = new JsonObject();
		square.addProperty("text", SWATCH);
		square.addProperty("color", String.format("#%06X", color.getTextureDiffuseColor() & 0xFFFFFF));

		JsonArray extra = new JsonArray();
		extra.add(square);
		extra.add(translated("color.minecraft." + color.getSerializedName()));

		JsonObject swatch = literal(prefix);
		swatch.add("extra", extra);
		return swatch;
	}

	private static JsonObject literal(String text) {
		JsonObject component = new JsonObject();
		component.addProperty("text", text);
		return component;
	}

	private static JsonObject literal(String text, String color) {
		JsonObject component = literal(text);
		component.addProperty("color", color);
		return component;
	}

	private static JsonObject translated(String key) {
		JsonObject component = new JsonObject();
		component.addProperty("translate", key);
		return component;
	}
}
