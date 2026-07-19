
// helpers that raise an exception when called

DEF_HELPER_1(unsupported, noreturn, env)
[# th:each="exc : ${exc_info.exceptions}"]
[(${exc.helper_def})]
[/]

// helper definitions for instructions
[# th:each="instr : ${instr_helper_defs}"]
[(${instr})]
[/]

// float helpers

DEF_HELPER_FLAGS_3(fadd_ieee32, TCG_CALL_NO_RWG, i32, env, i32, i32)