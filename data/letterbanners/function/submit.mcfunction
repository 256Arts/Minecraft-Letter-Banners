# Called by the dialog's Create button with {text, fg, bg, fade, size}.
# Spells the whole line, one banner per character, then plays a single sound.
$data modify storage letterbanners:io spell set value {text:"$(text)",fg:"$(fg)",bg:"$(bg)",fade:"$(fade)",size:"$(size)"}
scoreboard players set #given letterbanners 0
execute unless data storage letterbanners:io {spell:{text:""}} run function letterbanners:internal/spell
execute if score #given letterbanners matches 1.. run playsound minecraft:ui.loom.take_result master @s
