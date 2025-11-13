
// helpers that raise an exception when called

DEF_HELPER_1(unsupported, noreturn, env)
[# th:each="exc : ${exc_info.exceptions}"]
[(${exc.helper_def})]
[/]

[# th:each="instr : ${instr_helper_defs}"]// helper definitions for instructions
[(${instr})]
[/]