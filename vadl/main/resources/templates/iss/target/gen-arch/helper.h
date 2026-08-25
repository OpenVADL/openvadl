
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

#define TS i[(${target_size})]
[# th:each="c : ${float_builtins.fsqrt}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_fsqrt, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fadd}"]DEF_HELPER_FLAGS_4([(${c[0].name})]_fadd, 0, TS, env, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fsub}"]DEF_HELPER_FLAGS_4([(${c[0].name})]_fsub, 0, TS, env, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fmul}"]DEF_HELPER_FLAGS_4([(${c[0].name})]_fmul, 0, TS, env, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fdiv}"]DEF_HELPER_FLAGS_4([(${c[0].name})]_fdiv, 0, TS, env, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fmadd}"]DEF_HELPER_FLAGS_5([(${c[0].name})]_fmadd, 0, TS, env, TS, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fmsub}"]DEF_HELPER_FLAGS_5([(${c[0].name})]_fmsub, 0, TS, env, TS, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fnmadd}"]DEF_HELPER_FLAGS_5([(${c[0].name})]_fnmadd, 0, TS, env, TS, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fnmsub}"]DEF_HELPER_FLAGS_5([(${c[0].name})]_fnmsub, 0, TS, env, TS, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fmin}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_fmin, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fmax}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_fmax, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.flt}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_flt, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fle}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_fle, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.feq}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_feq, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fcvt}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_[(${c[1].name})]_fcvt, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fcvtfs}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_[(${c[1]})]_fcvtfs, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fcvtfu}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_[(${c[1]})]_fcvtfu, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fcvtsf}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_[(${c[1]})]_fcvtsf, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fcvtuf}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_[(${c[1]})]_fcvtuf, 0, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fisinf}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fisinf, 0, TS, env, TS)
[/][# th:each="c : ${float_builtins.fiszero}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fiszero, 0, TS, env, TS)
[/][# th:each="c : ${float_builtins.fisneg}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fisneg, 0, TS, env, TS)
[/][# th:each="c : ${float_builtins.fisdenorm}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fisdenorm, 0, TS, env, TS)
[/][# th:each="c : ${float_builtins.fissnan}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fissnan, 0, TS, env, TS)
[/][# th:each="c : ${float_builtins.fisqnan}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fisqnan, 0, TS, env, TS)
[/]