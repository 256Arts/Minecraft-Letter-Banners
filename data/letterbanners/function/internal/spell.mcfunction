# One character per pass: peel the first character off spell.text, hand it to
# create, then recurse while anything is left. The loop keeps its own storage
# because create overwrites args.
data modify storage letterbanners:io spell.letter set string storage letterbanners:io spell.text 0 1
data modify storage letterbanners:io spell.rest set string storage letterbanners:io spell.text 1
data modify storage letterbanners:io spell.text set from storage letterbanners:io spell.rest
function letterbanners:internal/spell_one with storage letterbanners:io spell
execute if score #valid letterbanners matches 1 run scoreboard players add #given letterbanners 1
execute unless data storage letterbanners:io {spell:{text:""}} run function letterbanners:internal/spell
