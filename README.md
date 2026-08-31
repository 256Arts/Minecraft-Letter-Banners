# Letter Banners

A Minecraft Java data pack that builds alphabet banners out of vanilla banner
patterns. Open a native in-game dialog, type some text, pick two dye colors, and
one banner per character lands in your inventory.

```mcfunction
/function letterbanner
```

## What you get

- **Text** — up to 32 characters of `A`–`Z`, `0`–`9` and spaces; you get one
  banner per character, and a space is a blank banner.
- **Text Color** — the dye the glyph is drawn in.
- **Background Color** — the banner's base dye.
- **Font Size** — `Large` fills the banner edge to edge; `Small` adds a
  `minecraft:border` layer in the background color on top, cropping the strokes
  inward so the character sits inside a margin.
- **Fade** — `None`, or one gradient layer in the background color across the
  whole banner: `Top` adds a `minecraft:gradient`, solid along the top edge and
  fading down over the glyph, and `Bottom` the mirror of it, a
  `minecraft:gradient_up` fading up from the bottom edge.

A row of three quick buttons — **A-Z**, **0-9** and **Symbols** — sits with
Create, and each gives that whole set in one click, in the colors and font size
picked above, without typing anything. The symbols set includes `"` and `\`,
which the text box cannot carry.

The dialog closes once the banners are in your inventory. Press Escape to close
it without making any, and run the command again for another word.

## Install

### As a data pack

1. Copy this repository's contents (`pack.mcmeta` and `data/`) into
   `<world>/datapacks/letter-banners/`, or zip them and drop the zip in the same
   folder.
2. `/reload`, or rejoin the world.
3. `/function letterbanner`

Requires **Minecraft Java 1.21.6 or newer** — dialogs do not exist before that.
Operator permission level 2 is needed, both for `/function` and because the
dialog's button runs a command as the player.

### As a mod

The `mod/` folder wraps the same data pack in a Fabric mod and a NeoForge mod,
so there is nothing to copy into the world folder and you get a real command
instead of `/function`. Drop the jar for your loader in `mods/` — the Fabric jar
needs Fabric API beside it, the NeoForge jar needs nothing — on the client for
singleplayer or on the server for multiplayer.

```
/letterbanner                                          opens the dialog
/letterbanner <text>                                   black on white, large
/letterbanner <text> <textColor>
/letterbanner <text> <textColor> <backgroundColor>
/letterbanner <text> <textColor> <backgroundColor> <large|small> [none|top|bottom]
```

`<text>` may be a single character or a whole word — quote it (`"GAME OVER"`) to
include spaces. Colors are dye names (`light_blue`, `magenta`, …) and the
command is case-insensitive. An unsupported character fails the whole command
rather than handing you half a word. Like the data pack, it needs permission
level 2.

The mod also builds its own dialog, whose buttons hand the inputs straight to
the mod instead of running a command, so pressing Create no longer raises the
client's "are you sure you want to run a command?" screen. Because that dialog
is rebuilt on every click, the two colors are picked from a row of dye buttons
that shows which dye is currently chosen, rather than from the data pack's cycle
buttons. Installing the data pack by hand on top of the mod puts the
command-running dialog back.

Build the jar with a network connection; Gradle fetches the JDK it needs:

```sh
cd mod
./gradlew build      # fabric/build/libs/letter-banners-fabric-<version>.jar
                     # neoforge/build/libs/letter-banners-neoforge-<version>.jar
```

One `./gradlew build` builds both jars. Everything but the entry point is plain
Minecraft code, so both loader projects compile the same sources out of
`mod/common/` and only their manifest and their one entry-point class differ.

Built against Minecraft 26.2, Fabric API 0.158.0 and NeoForge 26.2.0.75; the
versions live in `mod/gradle.properties`.

## How it works

Every glyph is a stack of vanilla banner patterns on a banner whose base color
is your background dye. Layers painted in the text color draw the strokes;
layers painted back in the background color carve pieces away. `G`, for
example, draws a right bar and a full middle bar, wipes the left half to shorten
that middle bar, then draws a `C` around it:

```
stripe_right (text) → stripe_middle (text) → half_vertical (bg)
→ stripe_left (text) → stripe_top (text) → stripe_bottom (text)
```

Nothing here is a custom texture or a resource pack — these are ordinary vanilla
banners, and they render on banner blocks, in item frames, and on shields.

Glyphs use up to seven layers, more than the six the loom allows, so they cannot
be reproduced in survival crafting.

## Repository layout

| Path | Purpose |
| --- | --- |
| `data/minecraft/function/letterbanner.mcfunction` | Entry point, so `/function letterbanner` works unqualified |
| `data/letterbanners/dialog/letter_banner.json` | The dialog definition |
| `data/letterbanners/function/submit.mcfunction` | Receives the dialog's inputs and spells the whole line |
| `data/letterbanners/function/create.mcfunction` | Builds one banner; the entry point the mod calls per character |
| `data/letterbanners/function/sets/` | One function per quick button: the whole set, then the sound |
| `mod/common/` | Mod code both loaders compile — the command, the dialog and the mixin |
| `mod/fabric/`, `mod/neoforge/` | Per-loader manifest, entry point and build |
| `data/letterbanners/function/internal/` | Spelling loop, character normalizing, validation, dispatch |
| `data/letterbanners/function/letters/` | One macro function per glyph |
| `.github/workflows/publish.yml` | Tag-triggered release to GitHub, Modrinth and CurseForge |
| `tools/generate.py` | Generates `letters/`, `sets/`, `internal/normalize.mcfunction`, and the dialog |

Edit glyph designs in `tools/generate.py` and re-run it; do not hand-edit the
generated files.

## Releasing

Pushing a `v<version>` tag (`v1.0.0`) runs `.github/workflows/publish.yml`,
which builds both jars, zips the data pack, and hands all three to
[mc-publish](https://github.com/marketplace/actions/mc-publish): one GitHub
release with generated notes holding everything, plus a Modrinth and a
CurseForge version per download (`<version>+fabric`, `+neoforge`, `+datapack`),
since only the Fabric jar depends on Fabric API. The tag has to match `version`
in `mod/gradle.properties` or the workflow stops before building.

Modrinth and CurseForge are each opt-in — either is skipped unless both of its
settings are present, so the workflow already works with none of them set up. To
turn one on, add its repository variable and secret under **Settings → Secrets
and variables → Actions**:

| Platform | Variable | Secret |
| --- | --- | --- |
| Modrinth | `MODRINTH_ID` (project id) | `MODRINTH_TOKEN` (PAT with **Create versions**) |
| CurseForge | `CURSEFORGE_ID` (project id) | `CURSEFORGE_TOKEN` (API token) |

## License

CC0 1.0 Universal — public domain dedication. See [LICENSE](LICENSE). Do
whatever you like with it.
