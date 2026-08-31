# Macro arguments are bound when a function is called, so peeling a character
# off the text and spending it cannot live in the same function.
$function letterbanners:create {letter:"$(letter)",fg:"$(fg)",bg:"$(bg)",fade:"$(fade)",size:"$(size)"}
