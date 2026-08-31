package com.arts256.letterbanners;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

import com.arts256.letterbanners.LetterBannerDialogs.CharacterSet;
import com.arts256.letterbanners.LetterBannerDialogs.Fade;

/**
 * {@code /letterbanner} — the mod's front door to the bundled data pack.
 *
 * <p>With no arguments it opens the dialog, which is what
 * {@code /function letterbanner} runs too. With arguments it spells out the whole
 * text without opening anything.
 */
public final class LetterBannerCommand {
	private static final Identifier CREATE_FUNCTION = Identifier.fromNamespaceAndPath(LetterBanners.MOD_ID, "create");

	/** Everything normalize.mcfunction knows how to fold down to a glyph. */
	private static final String SUPPORTED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !%^_-[]|:;=\"',.+()/\\";

	private static final SimpleCommandExceptionType DATA_PACK_MISSING = new SimpleCommandExceptionType(
			Component.literal("The Letter Banners data pack is not loaded. Try /reload."));
	private static final DynamicCommandExceptionType UNSUPPORTED_CHARACTER = new DynamicCommandExceptionType(
			character -> Component.literal("No banner design for \"" + character + "\". Use A-Z, 0-9, a symbol or a space."));
	private static final DynamicCommandExceptionType UNKNOWN_COLOR = new DynamicCommandExceptionType(
			name -> Component.literal("Unknown dye color \"" + name + "\"."));
	private static final DynamicCommandExceptionType UNKNOWN_SET = new DynamicCommandExceptionType(
			set -> Component.literal("No character set called \"" + set + "\"."));
	private static final SimpleCommandExceptionType TEXT_EMPTY = new SimpleCommandExceptionType(
			Component.literal("Type some text to spell out."));

	private static final String[] DYE_NAMES = Arrays.stream(DyeColor.values()).map(DyeColor::getSerializedName).toArray(String[]::new);
	private static final SuggestionProvider<CommandSourceStack> DYE_COLORS = (context, builder) -> SharedSuggestionProvider.suggest(DYE_NAMES, builder);

	private LetterBannerCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("letterbanner")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> openDialog(context.getSource()))
				.then(Commands.argument("text", StringArgumentType.string())
						.executes(context -> give(context, DyeColor.BLACK, DyeColor.WHITE, "large", Fade.NONE))
						.then(Commands.argument("textColor", StringArgumentType.word())
								.suggests(DYE_COLORS)
								.executes(context -> give(context, color(context, "textColor"), DyeColor.WHITE, "large", Fade.NONE))
								.then(Commands.argument("backgroundColor", StringArgumentType.word())
										.suggests(DYE_COLORS)
										.executes(context -> give(context, color(context, "textColor"), color(context, "backgroundColor"), "large", Fade.NONE))
										.then(size("large"))
										.then(size("small"))))));
	}

	/**
	 * A font size literal, optionally followed by which edge the banner fades
	 * from. A literal per fade rather than an argument, so the game completes
	 * them and rejects anything else before the command runs.
	 */
	private static LiteralArgumentBuilder<CommandSourceStack> size(String size) {
		LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(size)
				.executes(context -> give(context, color(context, "textColor"), color(context, "backgroundColor"), size, Fade.NONE));

		for (Fade fade : Fade.values()) {
			literal.then(Commands.literal(fade.id())
					.executes(context -> give(context, color(context, "textColor"), color(context, "backgroundColor"), size, fade)));
		}

		return literal;
	}

	private static int openDialog(CommandSourceStack source) throws CommandSyntaxException {
		// Built on the spot rather than looked up in the dialog registry, because
		// it is rebuilt on every click to show the colors the player has picked.
		LetterBannerDialogs.openMain(source.getPlayerOrException(), LetterBannerDialogs.State.DEFAULT);
		return 1;
	}

	private static int give(CommandContext<CommandSourceStack> context, DyeColor textColor, DyeColor backgroundColor, String size, Fade fade)
			throws CommandSyntaxException {
		return giveAndReport(context.getSource(), StringArgumentType.getString(context, "text"), textColor, backgroundColor, size, fade);
	}

	/**
	 * Spells {@code text} and reports the whole word in one line.
	 *
	 * <p>Shared with {@link LetterBannerDialog}, whose Create button spells a
	 * whole line too.
	 */
	static int giveAndReport(CommandSourceStack source, String text, DyeColor textColor, DyeColor backgroundColor, String size, Fade fade)
			throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		// Output is suppressed because otherwise a word is one "Gave 1 banner"
		// line per letter, and one summary reads better than twenty.
		int count = give(source.withSuppressedOutput(), text, textColor, backgroundColor, size, fade);

		return report(source, player, count);
	}

	/**
	 * Spells one whole character set for the dialog's quick buttons.
	 *
	 * <p>Runs the same generated {@code letterbanners:sets/...} function the data
	 * pack's own buttons run, which hands create a glyph id per character rather
	 * than a typed one -- which is how the symbols set can hold {@code "} and
	 * {@code \}, neither of which survives the text box.
	 */
	static int giveSet(CommandSourceStack source, String set, DyeColor textColor, DyeColor backgroundColor, String size, Fade fade)
			throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		// The id arrives in a packet any client can send and is spliced into a
		// command, so it is only ever one of the buttons the dialog offered.
		CharacterSet characters = LetterBannerDialogs.CHARACTER_SETS.stream()
				.filter(candidate -> candidate.id().equals(set))
				.findFirst()
				.orElseThrow(() -> UNKNOWN_SET.create(set));

		Identifier function = Identifier.fromNamespaceAndPath(LetterBanners.MOD_ID, "sets/" + characters.id());

		if (source.getServer().getFunctions().get(function).isEmpty()) {
			throw DATA_PACK_MISSING.create();
		}

		// Suppressed for the same reason the text path suppresses it: one summary
		// reads better than one "Gave 1 banner" line per character.
		source.getServer().getCommands().performPrefixedCommand(source.withSuppressedOutput(),
				"function %s {fg:\"%s\",bg:\"%s\",%s,size:\"%s\"}".formatted(
						function, textColor.getSerializedName(), backgroundColor.getSerializedName(), fade.arguments(), size));
		return report(source, player, characters.count());
	}

	/** The one line every give path ends on. */
	private static int report(CommandSourceStack source, ServerPlayer player, int count) {
		source.sendSuccess(() -> Component.literal("Gave %d letter banner%s to %s".formatted(count, count == 1 ? "" : "s", player.getName().getString())), true);
		return count;
	}

	/**
	 * Gives one banner per character of {@code text} and plays the finishing
	 * sound, returning how many were given.
	 */
	static int give(CommandSourceStack source, String text, DyeColor textColor, DyeColor backgroundColor, String size, Fade fade)
			throws CommandSyntaxException {
		String characters = text.toUpperCase(Locale.ROOT);

		// Validate before giving anything, so a typo halfway through a word does
		// not leave the player holding half of it.
		if (characters.isEmpty()) {
			throw TEXT_EMPTY.create();
		}

		for (int index = 0; index < characters.length(); index++) {
			char character = characters.charAt(index);

			if (SUPPORTED_CHARACTERS.indexOf(character) < 0) {
				throw UNSUPPORTED_CHARACTER.create(String.valueOf(character));
			}
		}

		if (source.getServer().getFunctions().get(CREATE_FUNCTION).isEmpty()) {
			throw DATA_PACK_MISSING.create();
		}

		// The glyph designs live in the data pack, so hand each character back to
		// it rather than rebuilding the pattern stacks here.
		Commands commands = source.getServer().getCommands();

		for (int index = 0; index < characters.length(); index++) {
			commands.performPrefixedCommand(source, "function %s {letter:\"%s\",fg:\"%s\",bg:\"%s\",%s,size:\"%s\"}".formatted(
					CREATE_FUNCTION, snbt(characters.charAt(index)), textColor.getSerializedName(), backgroundColor.getSerializedName(),
					fade.arguments(), size));
		}

		commands.performPrefixedCommand(source, "playsound minecraft:ui.loom.take_result master @s");
		return characters.length();
	}

	/** A character as it has to be spelled inside the quoted SNBT argument. */
	private static String snbt(char character) {
		return switch (character) {
			case '\\' -> "\\\\";
			case '"' -> "\\\"";
			default -> String.valueOf(character);
		};
	}

	private static DyeColor color(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, argument).toLowerCase(Locale.ROOT);
		DyeColor color = DyeColor.byName(name, null);

		if (color == null) {
			throw UNKNOWN_COLOR.create(name);
		}

		return color;
	}
}
