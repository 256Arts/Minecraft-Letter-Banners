# Builds one banner for the caller. Called with {letter, fg, bg, fade, size},
# where "fade" is none, top or bottom. Split out of submit.mcfunction so the
# Fabric mod's /letterbanner command can spell a whole word and play a single
# sound at the end.
$data modify storage letterbanners:io args set value {letter:"$(letter)",fg:"$(fg)",bg:"$(bg)",fade:"$(fade)",size:"$(size)",dir:"letters",extra:""}
function letterbanners:internal/normalize
execute if data storage letterbanners:io {args:{size:"small"}} run data modify storage letterbanners:io args.dir set value "letters_small"
execute if data storage letterbanners:io {args:{fade:"top"}} run data modify storage letterbanners:io args.pattern set value "gradient"
execute if data storage letterbanners:io {args:{fade:"bottom"}} run data modify storage letterbanners:io args.pattern set value "gradient_up"
execute if data storage letterbanners:io args.pattern run function letterbanners:internal/fade with storage letterbanners:io args
execute if score #valid letterbanners matches 1 run function letterbanners:internal/dispatch with storage letterbanners:io args
execute if score #valid letterbanners matches 0 run function letterbanners:internal/unknown with storage letterbanners:io args
