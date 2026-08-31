# Letter Banners

Minecraft Java data pack. Entry point `/function letterbanner` opens a vanilla
dialog that gives alphabet banners built from vanilla banner patterns.

- `data/letterbanners/function/letters/`, `letters_small/`, `sets/`,
  `internal/normalize.mcfunction`, and `data/letterbanners/dialog/letter_banner.json`
  are **generated**. Edit `tools/generate.py` and run `python3 tools/generate.py`
  instead.
- Glyphs are layer stacks: `fg` layers draw strokes, `bg` layers carve them back
  out. Later layers paint over earlier ones. A stack starting with a `base` layer
  in `fg` is an inverted design -- flood the banner in the text color and carve
  the glyph out of it.
- Symbols are glyphs too, under spelled-out names (`period`, `paren_left`, ...),
  since a function name only takes `[a-z0-9_.-]`. `SYMBOLS` in `tools/generate.py`
  maps the typed character to the name, and the mod's `SUPPORTED_CHARACTERS` has
  to match. The data pack's dialog cannot carry `"` or `\` -- the client splices
  the typed text into a command and macros splice it into SNBT -- so those two
  only work through the mod, which escapes them.
- The dialog's quick buttons spell a whole set (`SETS` in `tools/generate.py`,
  `CHARACTER_SETS` in `LetterBannerDialogs`): one generated macro function per
  set under `function/sets/`, calling create per **glyph id** rather than per
  typed character, so the symbols set holds `"` and `\` too. The data pack's
  buttons run those functions directly; the mod's send a `give_set` custom click
  action, and `giveSet` checks the id against `CHARACTER_SETS` before splicing it
  into a command, since any client can send that packet.
- Letter functions are macro functions taking `{letter, fg, bg, extra}`;
  `extra` is raw SNBT spliced into the `banner_patterns` list (leading comma).
  `fade` is `none`, `top` or `bottom`; `create` turns it into a pattern name and
  `internal/fade` writes that one gradient layer into `extra`. One side, not a
  toggle each: both gradients at once leave the background solid along both
  edges and bury the glyph.
- Font size picks the folder: `dir` in `args` is `letters` or `letters_small`,
  and `internal/dispatch.mcfunction` interpolates it. Small glyphs end with a
  `border` layer in the background color, cropping the strokes inward; their
  layer stacks come from gamergeeks.net's banner letters/numbers pages, which
  are drawn for exactly that crop.
- Targets 1.21.6+ (dialogs). Keep `pack.mcmeta` covering old `pack_format` and
  1.21.9+ `min_format`/`max_format`.
- `letterbanners:create` builds one banner and is the shared entry point;
  `submit` spells the dialog's whole text and plays one sound at the end.
  `internal/spell` peels one character off at a time and recurses -- macro
  arguments are bound when a function is called, so the character it peeled has
  to be spent by a second function, `internal/spell_one`.
- `mod/` wraps the same data pack in a Fabric and a NeoForge mod (`processResources`
  pulls `../data` into each jar; `pack.mcmeta` is left out so the loader generates
  a matching one) and adds `/letterbanner`, which calls `letterbanners:create` per
  character. Build both with `cd mod && ./gradlew build`; versions in
  `mod/gradle.properties`.
- The mod is multi-loader without Architectury: `mod/common/src/main` is not a
  Gradle project, it is a source directory both `mod/fabric` and `mod/neoforge`
  add to their own source set (see the `subprojects` block in `mod/build.gradle`,
  which also holds the shared `processResources` and Java config). Everything but
  the entry point is plain Mojang-mapped Minecraft code, so it compiles unchanged
  on both: only `LetterBannersFabric` (`ModInitializer` +
  `CommandRegistrationCallback`, hence the `fabric-command-api-v2` dependency) and
  `LetterBannersNeoForge` (`@Mod` + `RegisterCommandsEvent` on the game bus, no
  extra dependency) differ, plus `fabric.mod.json` vs `META-INF/neoforge.mods.toml`.
  Both manifests are expanded from `gradle.properties`, and both name
  `letterbanners.mixins.json`, which lives in `common`.
- Releases go out on a `v<version>` tag push: `.github/workflows/publish.yml`
  builds both jars, zips `pack.mcmeta` + `data/` beside them, and mc-publish ships
  all three to GitHub Releases, Modrinth and CurseForge. Version and game versions
  are read from `mod/gradle.properties`, and the tag has to match `version` there.
  One mc-publish step makes the GitHub release with every file; the three after it
  make one Modrinth/CurseForge version per download (`+fabric`, `+neoforge`,
  `+datapack` suffixes, and no GitHub token so they do not touch the release),
  because a version carries one loader list and one dependency list and only the
  Fabric jar needs `fabric-api`. The `+datapack` step is Modrinth-only. That dependency is declared in the workflow
  rather than read from `fabric.mod.json`, whose `fabric-command-api-v2` is not a
  project on either platform.
- Four mc-publish rules the split has to keep obeying. One: never widen a
  version's `loaders`, and never put the zip and a jar in the same version —
  Modrinth runs every validator whose loader the version declares over every file
  whose extension matches, and its NeoForge validator takes `.zip` as well as
  `.jar`, so a zip alongside `neoforge` is rejected for having no
  `neoforge.mods.toml`. Two: `environment` is a single flag value (`both`), not a
  block list, or the run dies on `Cannot convert "environment" to
  "LoaderEnvironmentType"`; the data pack step needs it spelled out because a bare
  zip has no manifest to infer it from. Three: each platform's token is passed
  only when its id variable is set, because mc-publish skips a platform with no
  token but guesses a slug — and then fails — when it has a token and no id. The
  tokens are organization secrets, so every repo has them. Four: the data pack
  cannot go to CurseForge at all. Its modloader group is Forge/Fabric/NeoForge/Quilt
  with no data pack tag, and mc-publish only sends the environment and Java tags
  when at least one loader matched, so CurseForge rejects the upload with
  `You must select at least one version from the environment group of versions`.
  CurseForge users get the data pack inside either jar.
- The data pack's dialog can only run a command, which the client guards with a
  "run a command?" screen, and being a static file it cannot show what the player
  has picked -- so its colors are cycle buttons labelled with a coloured square
  plus the dye's name.
- The mod builds its own dialog in Java instead (`LetterBannerDialogs`), sent as
  a direct `Holder<Dialog>` without registering it, so it can be rebuilt on every
  click. Colors are a row of square buttons labelled with the dye's item sprite
  (`{"type":"object","object":"atlas","atlas":"minecraft:items","sprite":"minecraft:item/red_dye"}`
  -- the atlas has to be named, since it defaults to the block atlas, which item
  textures are not in) in a palette dialog. `processResources` drops the data pack's dialog and its
  `menu.mcfunction`, and the mod ships a `menu.mcfunction` that runs
  `/letterbanner` instead.
- Giving the banners closes the dialog. The data pack's is `after_action: "close"`,
  which the client applies to every button -- fine there, since all four of its
  buttons give banners and its colors are inputs, not buttons. The mod's stays on
  `"none"` and closes itself with `ClientboundClearDialogPacket` after a
  successful give, because `"close"` would also fire on the two color buttons,
  whose whole job is to open the palette. A give that throws leaves the dialog up
  with the text still in it.
- No dialog state is kept server-side: each button carries the whole state in its
  `additions`, and a click's payload is those additions plus the dialog's inputs.
  Buttons send `letterbanners:submit`, `pick_color` or `set_color` custom click
  actions, picked up by a mixin on `ServerCommonPacketListenerImpl` and handled in
  `LetterBannerDialog`. Vanilla drops custom click actions and Fabric API has no
  hook for them, hence the mixin.
