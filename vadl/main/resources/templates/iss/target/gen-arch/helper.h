
// helper that raises an exception when called
DEF_HELPER_2(raise_exception, noreturn, env, i32)

DEF_HELPER_1(unsupported, noreturn, env)

DEF_HELPER_3(csrrw, tl, env, int, tl)

