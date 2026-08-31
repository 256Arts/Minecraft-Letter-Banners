# Appended verbatim inside the banner_patterns list, so it needs the leading
# comma. "pattern" is gradient, solid along the top edge and fading down, or
# gradient_up, its mirror. Only ever one of them: both at once would leave the
# background solid at both edges and bury the glyph, so the dialog picks a side
# rather than offering a toggle each.
$data modify storage letterbanners:io args.extra set value ',{pattern:"minecraft:$(pattern)",color:"$(bg)"}'
