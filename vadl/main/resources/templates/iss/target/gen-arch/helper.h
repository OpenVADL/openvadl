
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

[# th:each="fmt : ${float_formats}"]
DEF_HELPER_FLAGS_3(fadd_[(${fmt.name})], 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])[/]
