
// helpers that raise an exception when called

DEF_HELPER_1(unsupported, noreturn, env)
[# th:each="exc : ${exc_info.exceptions}"]
[(${exc.helper_def})]
[/]

// helper definitions for instructions
[# th:each="instr : ${instr_helper_defs}"]
[(${instr})]
[/]

[# th:if="${float_facts.has_float_ops}"]
// float helpers

#define TS i[(${target_size})]
[# th:each="c : ${float_builtins.fsqrt}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_fsqrt, TCG_CALL_NO_REG, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fadd}"]DEF_HELPER_FLAGS_4([(${c[0].name})]_fadd, TCG_CALL_NO_REG, TS, env, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fsub}"]DEF_HELPER_FLAGS_4([(${c[0].name})]_fsub, TCG_CALL_NO_REG, TS, env, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fmul}"]DEF_HELPER_FLAGS_4([(${c[0].name})]_fmul, TCG_CALL_NO_REG, TS, env, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fdiv}"]DEF_HELPER_FLAGS_4([(${c[0].name})]_fdiv, TCG_CALL_NO_REG, TS, env, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fmadd}"]DEF_HELPER_FLAGS_5([(${c[0].name})]_fmadd, TCG_CALL_NO_REG, TS, env, TS, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fmsub}"]DEF_HELPER_FLAGS_5([(${c[0].name})]_fmsub, TCG_CALL_NO_REG, TS, env, TS, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fnmadd}"]DEF_HELPER_FLAGS_5([(${c[0].name})]_fnmadd, TCG_CALL_NO_REG, TS, env, TS, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fnmsub}"]DEF_HELPER_FLAGS_5([(${c[0].name})]_fnmsub, TCG_CALL_NO_REG, TS, env, TS, TS, TS, TS)
[/][# th:each="c : ${float_builtins.fmin}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_fmin, TCG_CALL_NO_REG, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fmax}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_fmax, TCG_CALL_NO_REG, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.flt}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_flt, TCG_CALL_NO_REG, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fle}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_fle, TCG_CALL_NO_REG, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.feq}"]DEF_HELPER_FLAGS_3([(${c[0].name})]_feq, TCG_CALL_NO_REG, TS, env, TS, TS)
[/][# th:each="c : ${float_builtins.fcvt}"]
[# th:if='${c[0].type == "f" && c[1].type == "f"}']DEF_HELPER_FLAGS_3([(${c[0].name})]_[(${c[1].name})]_fcvt, TCG_CALL_NO_REG, TS, env, TS, TS)[/]
[# th:if='${c[0].type == "f" && c[1].type == "s"}']DEF_HELPER_FLAGS_3([(${c[0].name})]_[(${c[1].bit_size})]_fcvtfs, TCG_CALL_NO_REG, TS, env, TS, TS)[/]
[# th:if='${c[0].type == "f" && c[1].type == "u"}']DEF_HELPER_FLAGS_3([(${c[0].name})]_[(${c[1].bit_size})]_fcvtfu, TCG_CALL_NO_REG, TS, env, TS, TS)[/]
[# th:if='${c[0].type == "s" && c[1].type == "f"}']DEF_HELPER_FLAGS_3([(${c[1].name})]_[(${c[0].bit_size})]_fcvtsf, TCG_CALL_NO_REG, TS, env, TS, TS)[/]
[# th:if='${c[0].type == "u" && c[1].type == "f"}']DEF_HELPER_FLAGS_3([(${c[1].name})]_[(${c[0].bit_size})]_fcvtuf, TCG_CALL_NO_REG, TS, env, TS, TS)[/]
[/][# th:each="c : ${float_builtins.fisinf}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fisinf, TCG_CALL_NO_REG, TS, env, TS)
[/][# th:each="c : ${float_builtins.fiszero}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fiszero, TCG_CALL_NO_REG, TS, env, TS)
[/][# th:each="c : ${float_builtins.fisneg}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fisneg, TCG_CALL_NO_REG, TS, env, TS)
[/][# th:each="c : ${float_builtins.fisdenorm}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fisdenorm, TCG_CALL_NO_REG, TS, env, TS)
[/][# th:each="c : ${float_builtins.fissnan}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fissnan, TCG_CALL_NO_REG, TS, env, TS)
[/][# th:each="c : ${float_builtins.fisqnan}"]DEF_HELPER_FLAGS_2([(${c[0].name})]_fisqnan, TCG_CALL_NO_REG, TS, env, TS)
[/]

[/]
