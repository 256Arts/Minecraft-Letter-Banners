#!/usr/bin/env python3
"""Generates the letter functions and dispatch tables for the Letter Banners data pack.

Run from the repo root:  python3 tools/generate.py

Each glyph is a stack of vanilla banner patterns painted on a banner whose base
color is the player's background color. "fg" entries paint in the text color,
"bg" entries paint back in the background color to carve shapes away, so a
pattern list is read top-to-bottom like layers in an image editor. A glyph that
is drawn the other way round -- text color everywhere, shape carved out of it --
just starts with a "base" layer in the text color.
"""

import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NS = os.path.join(ROOT, "data", "letterbanners", "function")

FG, BG = "fg", "bg"

# Shorthands for the vanilla pattern registry ids.
LS = "stripe_left"           # vertical bar, left edge
RS = "stripe_right"          # vertical bar, right edge
CS = "stripe_center"         # vertical bar, center
TS = "stripe_top"            # horizontal bar, top edge
MS = "stripe_middle"         # horizontal bar, center
BS = "stripe_bottom"         # horizontal bar, bottom edge
DRS = "stripe_downright"     # diagonal, top-left to bottom-right
DLS = "stripe_downleft"      # diagonal, top-right to bottom-left
HV = "half_vertical"         # left half
HH = "half_horizontal"       # top half
HHB = "half_horizontal_bottom"  # bottom half
TL = "square_top_left"
TR = "square_top_right"
BL = "square_bottom_left"
BR = "square_bottom_right"
CROSS = "cross"              # diagonal X
BORDER = "border"            # thin outline around the whole banner
CBO = "curly_border"         # scalloped outline, bites into the edges
MR = "rhombus"               # diamond in the middle
TT = "triangle_top"          # triangle pointing down from the top edge
TTS = "triangles_top"        # sawtooth along the top edge
BT = "triangle_bottom"       # triangle pointing up from the bottom edge
BTS = "triangles_bottom"     # sawtooth along the bottom edge
BASE = "base"                # fills the whole banner

# Large font: the alphabet, digits and symbols as drawn on gamergeeks.net's
# banner pages, layer for layer. A stack that starts with `(BASE, FG)` is one of
# their inverted designs -- the banner is flooded with the text color and the
# glyph is carved back out of it in the background color.
GLYPHS = {
    "a": [(HH, FG), (MR, BG), (MS, FG), (RS, FG), (LS, FG)],
    # as D, plus the middle stripe splitting the bowl in two -- the table this
    # font came from spells B and D identically, which draws two Ds.
    "b": [(BASE, FG), (MR, BG), (HV, BG), (TS, FG), (BS, FG), (LS, FG), (MS, FG)],
    "c": [(LS, FG), (BS, FG), (TS, FG)],
    "d": [(BASE, FG), (MR, BG), (HV, BG), (TS, FG), (BS, FG), (LS, FG)],
    "e": [(TS, FG), (MS, FG), (BS, FG), (LS, FG)],
    "f": [(TS, FG), (MS, FG), (LS, FG)],
    "g": [(BASE, FG), (MR, BG), (HH, BG), (HV, BG), (BS, FG), (TS, FG), (LS, FG)],
    "h": [(RS, FG), (LS, FG), (MS, FG)],
    "i": [(CS, FG), (TS, FG), (BS, FG)],
    "j": [(BASE, FG), (HH, BG), (MR, BG), (RS, FG)],
    # the X's left half stays solid, giving a thick stem with two arms off it.
    "k": [(DRS, FG), (DLS, FG), (HV, FG)],
    "l": [(LS, FG), (BS, FG)],
    "m": [(TT, FG), (TTS, BG), (RS, FG), (LS, FG)],
    "n": [(RS, FG), (LS, FG), (DRS, FG)],
    "o": [(BS, FG), (TS, FG), (LS, FG), (RS, FG)],
    "p": [(HH, FG), (CS, BG), (TS, FG), (MS, FG), (LS, FG)],
    "q": [(BASE, FG), (MR, BG), (BR, FG), (HH, BG), (RS, FG), (LS, FG), (TS, FG)],
    "r": [(HH, FG), (CS, BG), (TS, FG), (MS, FG), (LS, FG), (DRS, FG)],
    "s": [(BASE, FG), (MR, BG), (MS, BG), (DRS, FG)],
    "t": [(CS, FG), (TS, FG)],
    "u": [(RS, FG), (LS, FG), (BS, FG)],
    "v": [(BASE, FG), (HH, BG), (MR, BG), (RS, FG), (LS, FG)],
    "w": [(BT, FG), (BTS, BG), (RS, FG), (LS, FG)],
    "x": [(DRS, FG), (DLS, FG)],
    "y": [(DRS, FG), (DLS, FG), (BR, BG)],
    "z": [(TS, FG), (BS, FG), (DLS, FG)],
    "0": [(BS, FG), (TS, FG), (LS, FG), (RS, FG)],
    "1": [(BS, FG), (CS, FG), (TL, FG)],
    "2": [(HH, FG), (MR, BG), (MS, BG), (DLS, FG), (BS, FG)],
    "3": [(RS, FG), (TS, FG), (MS, FG), (BS, FG)],
    "4": [(BASE, FG), (HH, BG), (BS, BG), (RS, FG), (LS, FG), (BL, BG)],
    "5": [(BASE, FG), (HH, BG), (MR, BG), (MS, BG), (DRS, FG), (TS, FG)],
    "6": [(BASE, FG), (HH, BG), (CS, BG), (BS, FG), (MS, FG), (LS, FG), (TS, FG)],
    "7": [(TS, FG), (DLS, FG)],
    "8": [(BS, FG), (TS, FG), (LS, FG), (RS, FG), (MS, FG)],
    "9": [(HH, FG), (CS, BG), (TS, FG), (MS, FG), (RS, FG)],
    # the center bar, cut twice: the lower cut leaves the dot under the gap.
    "exclamation": [(CS, FG), (MS, BG), (BS, BG)],
    "percent": [(TL, FG), (BR, FG), (CS, BG), (DLS, FG)],
    "caret": [(HH, FG), (CS, BG), (TS, FG)],
    "underscore": [(BS, FG)],
    "hyphen": [(MS, FG)],
    "bracket_left": [(LS, FG), (TS, FG), (BS, FG), (RS, BG)],
    "bracket_right": [(RS, FG), (TS, FG), (BS, FG), (LS, BG)],
    "pipe": [(CS, FG), (MS, BG)],
    "colon": [(TR, FG), (BR, FG)],
    "semicolon": [(RS, FG), (HH, BG), (TR, FG)],
    "equals": [(BASE, FG), (TS, BG), (BS, BG), (MS, BG)],
    "quote": [(TS, FG), (CS, BG)],
    "apostrophe": [(TS, FG), (HV, BG), (CS, BG)],
    "comma": [(RS, FG), (BS, FG), (HH, BG), (HV, BG)],
    "period": [(BR, FG)],
    "plus": [(CS, FG), (MS, FG)],
    "paren_left": [(BASE, FG), (RS, BG), (BS, BG), (LS, BG), (TS, BG), (TR, FG), (BR, FG)],
    "paren_right": [(BASE, FG), (RS, BG), (BS, BG), (LS, BG), (TS, BG), (TL, FG), (BL, FG)],
    "slash": [(DLS, FG)],
    "backslash": [(DRS, FG)],
    "space": [(BASE, BG)],
}

# Symbols the player can type, and the glyph id they are drawn by -- a function
# name can only hold [a-z0-9_.-], so every symbol needs a spelled-out one.
SYMBOLS = {
    "!": "exclamation", "%": "percent", "^": "caret", "_": "underscore",
    "-": "hyphen", "[": "bracket_left", "]": "bracket_right", "|": "pipe",
    ":": "colon", ";": "semicolon", "=": "equals", '"': "quote",
    "'": "apostrophe", ",": "comma", ".": "period", "+": "plus",
    "(": "paren_left", ")": "paren_right", "/": "slash", "\\": "backslash",
}

DISPLAY = dict({"space": "Blank"}, **{glyph: symbol for symbol, glyph in SYMBOLS.items()})

# Small font: `border` painted in the background color on top of the finished
# glyph crops every stroke away from the banner's edges, so the character reads
# a size smaller with a margin around it.
SMALL_CROP = (BORDER, BG)

# The small glyphs are the alphabet from gamergeeks.net's Minecraft banner
# letters and numbers pages, which are all drawn inside a background-colored
# `border` -- the same crop this font already applies, so the layers below stop
# just short of it. A few glyphs start by painting the whole banner in the text
# color and carving the shape back out of it.
SMALL_GLYPHS = {
    "a": [(RS, FG), (LS, FG), (MS, FG), (TS, FG)],
    "b": [(RS, FG), (BS, FG), (TS, FG), (CBO, BG), (LS, FG), (MS, FG)],
    "c": [(TS, FG), (BS, FG), (RS, FG), (MS, BG), (LS, FG)],
    "d": [(RS, FG), (BS, FG), (TS, FG), (CBO, BG), (LS, FG)],
    "e": [(LS, FG), (TS, FG), (MS, FG), (BS, FG)],
    "f": [(MS, FG), (RS, BG), (TS, FG), (LS, FG)],
    "g": [(RS, FG), (HH, BG), (BS, FG), (LS, FG), (TS, FG)],
    "h": [(BASE, FG), (TS, BG), (BS, BG), (LS, FG), (RS, FG)],
    "i": [(CS, FG), (TS, FG), (BS, FG)],
    "j": [(LS, FG), (HH, BG), (BS, FG), (RS, FG)],
    "k": [(DRS, FG), (HH, BG), (DLS, FG), (LS, FG)],
    "l": [(BS, FG), (LS, FG)],
    "m": [(TT, FG), (TTS, BG), (LS, FG), (RS, FG)],
    "n": [(LS, FG), (TT, BG), (DRS, FG), (RS, FG)],
    "o": [(LS, FG), (RS, FG), (BS, FG), (TS, FG)],
    "p": [(RS, FG), (HHB, BG), (MS, FG), (TS, FG), (LS, FG)],
    "q": [(BASE, FG), (MR, BG), (RS, FG), (LS, FG), (BR, FG)],
    "r": [(HH, FG), (CS, BG), (TS, FG), (LS, FG), (DRS, FG)],
    "s": [(BASE, FG), (MR, BG), (MS, BG), (DRS, FG)],
    "t": [(TS, FG), (CS, FG)],
    "u": [(BS, FG), (LS, FG), (RS, FG)],
    "v": [(DLS, FG), (LS, FG), (BT, BG), (DLS, FG)],
    "w": [(BT, FG), (BTS, BG), (LS, FG), (RS, FG)],
    "x": [(CROSS, FG)],
    "y": [(DRS, FG), (HHB, BG), (DLS, FG)],
    "z": [(TS, FG), (DLS, FG), (BS, FG)],
    "0": [(BS, FG), (LS, FG), (TS, FG), (RS, FG), (DLS, FG)],
    "1": [(CS, FG), (TL, FG), (CBO, BG), (BS, FG)],
    "2": [(TS, FG), (MR, BG), (BS, FG), (DLS, FG)],
    "3": [(BS, FG), (MS, FG), (TS, FG), (CBO, BG), (RS, FG)],
    "4": [(LS, FG), (HHB, BG), (RS, FG), (MS, FG)],
    "5": [(BS, FG), (MR, BG), (TS, FG), (DRS, FG)],
    "6": [(BS, FG), (RS, FG), (HH, BG), (MS, FG), (TS, FG), (LS, FG)],
    "7": [(DLS, FG), (TS, FG)],
    "8": [(TS, FG), (LS, FG), (MS, FG), (BS, FG), (RS, FG)],
    "9": [(LS, FG), (HHB, BG), (MS, FG), (TS, FG), (RS, FG), (BS, FG)],
}

# Characters the player can type, mapped to the function that draws them.
TYPED = {c: c for c in "abcdefghijklmnopqrstuvwxyz0123456789"}
TYPED.update({c.upper(): c for c in "abcdefghijklmnopqrstuvwxyz"})
TYPED.update(SYMBOLS)
TYPED[" "] = "space"

HEADER = "# Generated by tools/generate.py -- do not edit by hand.\n"

# Ordered the way the dialog lists them: greys, then the colour wheel.
DYES = [
    "white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
    "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink",
]

# DyeColor's texture colours, so a swatch matches the banner the dye paints.
# Dark dyes stay dark against the dialog, which is why the colour name is
# spelled out next to the swatch rather than replaced by it.
SWATCH_COLORS = {
    "white": "#F9FFFE", "light_gray": "#9D9D97", "gray": "#474F52", "black": "#1D1D21",
    "brown": "#835432", "red": "#B02E26", "orange": "#F9801D", "yellow": "#FED83D",
    "lime": "#80C71F", "green": "#5E7C16", "cyan": "#169C9C", "light_blue": "#3AB3DA",
    "blue": "#3C44AA", "purple": "#8932B8", "magenta": "#C74EBD", "pink": "#F38BAA",
}

SIZES = [("large", "Large"), ("small", "Small")]

# One gradient layer in the background color, or none. Top and bottom are one
# option rather than a checkbox each because a banner carrying both is solid
# background along both edges, with the glyph buried in between.
FADES = [("none", "None"), ("top", "Top"), ("bottom", "Bottom")]

# One banner per character, so a long line is a lot of banners and a lot of
# recursion in internal/spell.mcfunction. Long enough for a sign's worth of text.
TEXT_MAX_LENGTH = 32

SUBMIT = (
    'function letterbanners:submit '
    '{text:"$(text)",fg:"$(fg)",bg:"$(bg)",fade:"$(fade)",size:"$(size)"}'
)

# A data pack dialog can only ever run a command, which the client guards with
# an "are you sure you want to run a command?" screen. The mod builds its own
# dialog in Java instead -- swatch buttons, no warning screen -- so this file is
# the data pack's alone. See LetterBannerDialogs.java.
RUN_COMMAND_ACTION = {"type": "minecraft:dynamic/run_command", "template": SUBMIT}

# The quick buttons: one click spells a whole set, with no typing at all. Each
# one runs its own generated function under sets/, which calls create per glyph
# id -- ids rather than typed characters, so the symbols set can hold " and \,
# which the text box cannot carry. Keep in step with CHARACTER_SETS in
# LetterBannerDialogs.java, which draws the mod's copy of these buttons.
# The button labels are kept short because the three sit in one row: a dialog's
# action grid gives every column the width of its widest button, so a long label
# on one of them pushes all three wide.
SETS = [
    ("letters", "All Letters (A-Z)", "Make A-Z", list("abcdefghijklmnopqrstuvwxyz")),
    ("digits", "All Digits (0-9)", "Make 0-9", list("0123456789")),
    ("symbols", "All Symbols", "Make Symbols", list(SYMBOLS.values())),
]

# Three quick buttons to a row, spaced two apart, come out the width of the body
# above them -- wide enough for the longest label, "Make Symbols", to draw whole.
SET_BUTTON_WIDTH = 98

SET_COMMAND = ('function letterbanners:sets/%s '
               '{fg:"$(fg)",bg:"$(bg)",fade:"$(fade)",size:"$(size)"}')


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as handle:
        handle.write(text)


def glyph_function(name, layers):
    patterns = ",".join(
        '{pattern:"minecraft:%s",color:"$(%s)"}' % (pattern, slot)
        for pattern, slot in layers
    )
    label = DISPLAY.get(name, name.upper())
    return (
        HEADER
        + "# %s\n" % label
        + "$give @s minecraft:$(bg)_banner["
        + "minecraft:banner_patterns=[%s$(extra)]," % patterns
        + "minecraft:item_name=%s]\n" % json.dumps({"text": label, "italic": False})
    )


def set_function(label, glyphs):
    """One banner per character of a set, then the sound create leaves to its caller."""
    return (
        HEADER
        + "# %s\n" % label
        + "".join(
            '$function letterbanners:create '
            '{letter:"%s",fg:"$(fg)",bg:"$(bg)",fade:"$(fade)",size:"$(size)"}\n' % glyph
            for glyph in glyphs
        )
        + "playsound minecraft:ui.loom.take_result master @s\n"
    )


def set_buttons():
    return [
        {
            "label": {"text": button_label},
            "tooltip": {"text": "%s. One banner per character, %d in all, in the colors"
                                " picked above." % (label, len(glyphs))},
            "width": SET_BUTTON_WIDTH,
            "action": {"type": "minecraft:dynamic/run_command", "template": SET_COMMAND % name},
        }
        for name, label, button_label, glyphs in SETS
    ]


def literal_options(options, default):
    return [
        dict({"id": option, "display": {"text": label}},
             **({"initial": True} if option == default else {}))
        for option, label in options
    ]


def swatch(dye):
    """A square of the dye followed by its name.

    The square sits in "extra" rather than first in a list so that it is the
    only part carrying a colour: siblings inherit from their parent, so the
    name keeps whatever colour the button draws its label in.
    """
    return {
        "text": "",
        "extra": [
            {"text": "\u25a0 ", "color": SWATCH_COLORS[dye]},
            {"translate": "color.minecraft." + dye},
        ],
    }


def color_options(default):
    return [
        dict({"id": dye, "display": swatch(dye)},
             **({"initial": True} if dye == default else {}))
        for dye in DYES
    ]


def dialog():
    return {
        "type": "minecraft:multi_action",
        "title": {"text": "Letter Banners"},
        "external_title": {"text": "Letter Banners"},
        "can_close_with_escape": True,
        "pause": False,
        "after_action": "close",
        "body": [
            {
                "type": "minecraft:plain_message",
                "contents": {
                    "text": "Type letters (A-Z), digits (0-9), symbols and spaces,"
                            " pick your colors, and one banner per character is added"
                            " to your inventory.",
                    "color": "gray",
                },
                "width": 300,
            }
        ],
        "inputs": [
            {"type": "minecraft:text", "key": "text", "label": {"text": "Text"},
             "width": 200, "max_length": TEXT_MAX_LENGTH, "initial": "A"},
            {"type": "minecraft:single_option", "key": "fg", "label": {"text": "Text Color"},
             "width": 200, "options": color_options("black")},
            {"type": "minecraft:single_option", "key": "bg", "label": {"text": "Background Color"},
             "width": 200, "options": color_options("white")},
            {"type": "minecraft:single_option", "key": "size", "label": {"text": "Font Size"},
             "width": 200, "options": literal_options(SIZES, "large")},
            {"type": "minecraft:single_option", "key": "fade", "label": {"text": "Fade"},
             "width": 200, "options": literal_options(FADES, "none")},
        ],
        # Three columns, so the quick buttons make one row. The client lays the
        # actions out row by row and hands whatever is left over at the end a
        # centered row of its own, which is how Create keeps its full width
        # underneath them.
        "columns": 3,
        "actions": set_buttons() + [
            {
                "label": {"text": "Make Banners"},
                "tooltip": {"text": "One banner per character. The dialog closes once"
                                    " they are given."},
                "width": 200,
                "action": RUN_COMMAND_ACTION,
            }
        ],
    }


def main():
    for directory in ("letters", "letters_small", "sets"):
        path = os.path.join(NS, directory)
        if os.path.isdir(path):
            shutil.rmtree(path)
    for name, label, _button_label, glyphs in SETS:
        write(os.path.join(NS, "sets", name + ".mcfunction"), set_function(label, glyphs))
    for name, layers in GLYPHS.items():
        write(os.path.join(NS, "letters", name + ".mcfunction"),
              glyph_function(name, layers))
        small = SMALL_GLYPHS.get(name, layers) + [SMALL_CROP]
        write(os.path.join(NS, "letters_small", name + ".mcfunction"),
              glyph_function(name, small))

    lines = [
        HEADER,
        "# Folds the typed character down to a glyph id and flags whether it is one we can draw.\n",
        "scoreboard players set #valid letterbanners 0\n",
    ]
    for typed, glyph in sorted(TYPED.items()):
        typed_snbt = '"%s"' % typed.replace("\\", "\\\\").replace('"', '\\"')
        lines.append(
            'execute if data storage letterbanners:io {args:{letter:%s}} run '
            'data modify storage letterbanners:io args.letter set value "%s"\n'
            % (typed_snbt, glyph)
        )
    for glyph in sorted(GLYPHS):
        lines.append(
            'execute if data storage letterbanners:io {args:{letter:"%s"}} run '
            "scoreboard players set #valid letterbanners 1\n" % glyph
        )
    write(os.path.join(NS, "internal", "normalize.mcfunction"), "".join(lines))

    write(
        os.path.join(ROOT, "data", "letterbanners", "dialog", "letter_banner.json"),
        json.dumps(dialog(), indent=2) + "\n",
    )


if __name__ == "__main__":
    main()
