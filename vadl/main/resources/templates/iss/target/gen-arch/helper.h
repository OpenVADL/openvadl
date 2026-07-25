
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
DEF_HELPER_FLAGS_2([(${fmt.name})]_fsqrt, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_3([(${fmt.name})]_fadd, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_3([(${fmt.name})]_fsub, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_3([(${fmt.name})]_fmul, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_3([(${fmt.name})]_fdiv, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_4([(${fmt.name})]_fmadd, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_4([(${fmt.name})]_fmsub, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_4([(${fmt.name})]_fnmadd, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_4([(${fmt.name})]_fnmsub, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_3([(${fmt.name})]_fmin, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_3([(${fmt.name})]_fmax, 0, i[(${fmt.bit_size})], env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])

DEF_HELPER_FLAGS_3([(${fmt.name})]_flt, 0, i64, env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_3([(${fmt.name})]_fle, 0, i64, env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_3([(${fmt.name})]_feq, 0, i64, env, i[(${fmt.bit_size})], i[(${fmt.bit_size})])

DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtfss, 0, i32, env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtfsd, 0, i64, env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtfus, 0, i32, env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtfud, 0, i64, env, i[(${fmt.bit_size})])

// FIXME: this is currently very complex and will change
[# th:if="${fmt.bit_size != 64}"]
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtssf, 0, i32, env, i32)
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtsdf, 0, i32, env, i64)
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtusf, 0, i32, env, i32)
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtudf, 0, i32, env, i64)
[/]
[# th:if="${fmt.bit_size != 32}"]
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtssf2, 0, i64, env, i32)
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtsdf2, 0, i64, env, i64)
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtusf2, 0, i64, env, i32)
DEF_HELPER_FLAGS_2([(${fmt.name})]_fcvtudf2, 0, i64, env, i64)
[/]
[# th:each="fmt2 : ${float_formats}"][# th:if="${fmt.name != fmt2.name}"]
[# th:if="${fmt.bit_size != 32}"]DEF_HELPER_FLAGS_2([(${fmt.name})]_[(${fmt2.name})]_fcvtff, 0, i32, env, i[(${fmt.bit_size})])[/]
[# th:if="${fmt.bit_size != 64}"]DEF_HELPER_FLAGS_2([(${fmt.name})]_[(${fmt2.name})]_fcvtff2, 0, i64, env, i[(${fmt.bit_size})])[/]
[/][/]

DEF_HELPER_FLAGS_2([(${fmt.name})]_fisinf, 0, i64, env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_2([(${fmt.name})]_fiszero, 0, i64, env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_2([(${fmt.name})]_fisneg, 0, i64, env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_2([(${fmt.name})]_fisdenorm, 0, i64, env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_2([(${fmt.name})]_fissnan, 0, i64, env, i[(${fmt.bit_size})])
DEF_HELPER_FLAGS_2([(${fmt.name})]_fisqnan, 0, i64, env, i[(${fmt.bit_size})])
[/]
