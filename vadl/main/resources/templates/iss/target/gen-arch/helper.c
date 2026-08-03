#include "qemu/osdep.h"
#include "cpu.h"
#include "exec/exec-all.h"
#include "exec/cpu_ldst.h"
#include "exec/helper-proto.h"
#include "qemu/log-for-trace.h"
#include "qemu/qemu-print.h"
#include "cpu-bits.h"
#include "fpu/softfloat.h"
#include "vadl-builtins.h"
#include "vadl-iss-builtins.h"

static G_NORETURN void [(${gen_arch_lower})]_raise_exception(CPU[(${gen_arch_upper})]State *env, int32_t exception) {
    CPUState *cs = env_cpu(env);
    cs->exception_index = exception;
    cpu_loop_exit_restore(cs, 0);
}

// TODO: Remove unsupported exception once supported in spec
void helper_unsupported(CPU[(${gen_arch_upper})]State *env) {
    CPUState *cs = env_cpu(env);

    cs->exception_index = EXCP_HLT;

    cpu_loop_exit(cs);
}

[# th:each="exc : ${exc_info.exceptions}"]
[(${exc.helper_impl})]
[/]

// instr helper functions
[# th:each="instr : ${instr_helper_impls}"]
[(${instr})]
[/]

// float helpers

void prep_float_status(CPU[(${gen_arch_upper})]State *env, float_status *s, uint32_t rm) {
  uint16_t flags = 0xffff;
  // un-set non sticky flags
  [# th:each="reg : ${register_tensors}"][# th:each="flag : ${reg.non_sticky_fe_flags}"]
  flags &= ~(1 << [(${flag.flag_idx})]);[/][/]
  // un-set sticky flags that are not set
  [# th:each="reg : ${register_tensors}"][# th:each="flag : ${reg.sticky_fe_flags}"]
  flags &= ~((1 - ((env->[(${reg.name_lower})] >> [(${flag.idx})]) & 1)) << [(${flag.flag_idx})]);[/][/]
  set_float_exception_flags(flags, s);
  set_float_rounding_mode(rm, s);
  // TODO: this disables nan-propagation. this will be configurable via the vadl spec at some point
  set_default_nan_mode(1, s);
}

void set_float_status(CPU[(${gen_arch_upper})]State *env, float_status *s) {
  // DEV NOTE: for now we write directly to the flags register. This means that the helper is not pure and
  //           thus slower. In the future, we should optimize this.
  uint16_t flags = get_float_exception_flags(s);
  [# th:each="reg : ${register_tensors}"][# th:each="flag : ${reg.sticky_fe_flags}"]
  env->[(${reg.name_lower})] |= (flags >> [(${flag.flag_idx})] & 1) << [(${flag.idx})];[/][/]
  [# th:each="reg : ${register_tensors}"][# th:each="flag : ${reg.non_sticky_fe_flags}"]
  env->[(${reg.name_lower})] &= ~((1 - (flags >> [(${flag.flag_idx})] & 1)) << [(${flag.idx})]);[/][/]
}

#define FLOAT_HELPER_BODY(RET_TY, CALL, FMT, RM) \
  float_status *s = &env->fp_status_##FMT;                                                  \
  prep_float_status(env, s, RM);                                                            \
  RET_TY result = CALL;                                                                     \
  set_float_status(env, s);                                                                 \
  return result;

#define FLOAT_HELPER_1(S, FMT, NAME, QEMU_FUN) \
  uint##S##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,                     \
                                   uint##S##_t rs1, uint32_t rm) {                          \
    FLOAT_HELPER_BODY(uint##S##_t, float##S##_##QEMU_FUN(rs1, s), FMT, rm)                  \
  }

#define FLOAT_HELPER_2(S, FMT, NAME, QEMU_FUN) \
  uint##S##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,                     \
                                   uint##S##_t rs1, uint##S##_t rs2, uint32_t rm) {         \
    FLOAT_HELPER_BODY(uint##S##_t, float##S##_##QEMU_FUN(rs1, rs2, s), FMT, rm)             \
  }

#define FLOAT_HELPER_MINMAX(S, FMT, NAME, QEMU_FUN) \
  uint##S##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,                     \
                                   uint##S##_t rs1, uint##S##_t rs2) {                      \
    FLOAT_HELPER_BODY(uint##S##_t, float##S##_##QEMU_FUN(rs1, rs2, s), FMT, 0)              \
  }

#define FLOAT_HELPER_3(S, FMT, NAME, QEMU_FUN, FLAGS) \
  uint##S##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,                     \
                                   uint##S##_t rs1, uint##S##_t rs2, uint##S##_t rs3,       \
                                   uint32_t rm) {                                           \
    FLOAT_HELPER_BODY(uint##S##_t, float##S##_##QEMU_FUN(rs1, rs2, rs3, FLAGS, s), FMT, rm) \
  }

#define FLOAT_HELPER_F2I(S, FMT, INT_S, INT_FMT, NAME) \
  uint##INT_S##_t helper_##FMT##_##INT_S##_##NAME(CPU[(${gen_arch_upper})]State *env,       \
                                   uint##S##_t rs1, uint32_t rm) {                          \
    FLOAT_HELPER_BODY(uint##INT_S##_t, float##S##_to_##INT_FMT(rs1, s), FMT, rm)            \
  }

#define FLOAT_HELPER_I2F(S, FMT, INT_S, INT_FMT, NAME) \
  uint##S##_t helper_##FMT##_##INT_S##_##NAME(CPU[(${gen_arch_upper})]State *env,           \
                                   uint##INT_S##_t rs1, uint32_t rm) {                      \
    FLOAT_HELPER_BODY(uint##S##_t, INT_FMT##_to_##float##S(rs1, s), FMT, rm)                \
  }

#define FLOAT_HELPER_F2F(S, FMT, S2, FMT2, NAME) \
  uint##S2##_t helper_##FMT##_##FMT2##_##NAME(CPU[(${gen_arch_upper})]State *env,           \
                                   uint##S##_t rs1, uint32_t rm) {                          \
    FLOAT_HELPER_BODY(uint##S2##_t, float##S##_to_##float##S2(rs1, s), FMT, rm)             \
  }

// TODO: optimize fe flags (maybe prep can be omitted; or flags set to avoid recomputation)
#define FLOAT_HELPER_CMP(S, FMT, NAME, QEMU_FUN) \
  uint64_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,                        \
                                   uint##S##_t rs1, uint##S##_t rs2) {                      \
    FLOAT_HELPER_BODY(bool, float##S##_##QEMU_FUN(rs1, rs2, s), FMT, 0)                     \
  }

#define FLOAT_HELPER_CLASSS(S, FMT, NAME, QEMU_FUN) \
  uint64_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,                        \
                                   uint##S##_t rs1) {                                       \
    FLOAT_HELPER_BODY(bool, float##S##_##QEMU_FUN(rs1, s), FMT, 0)                          \
  }

#define FLOAT_HELPER_CLASS(S, FMT, NAME, QEMU_FUN) \
  uint64_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,                        \
                                   uint##S##_t rs1) {                                       \
    FLOAT_HELPER_BODY(bool, float##S##_##QEMU_FUN(rs1), FMT, 0)                             \
  }

// TODO: risc-v specifies eq as quiet. other ISAs might want to configure this

[# th:each="c : ${float_builtins.fsqrt}"]FLOAT_HELPER_1([(${c[0].bit_size})], [(${c[0].name})], fsqrt, sqrt)
[/][# th:each="c : ${float_builtins.fadd}"]FLOAT_HELPER_2([(${c[0].bit_size})], [(${c[0].name})], fadd, add)
[/][# th:each="c : ${float_builtins.fsub}"]FLOAT_HELPER_2([(${c[0].bit_size})], [(${c[0].name})], fsub, sub)
[/][# th:each="c : ${float_builtins.fmul}"]FLOAT_HELPER_2([(${c[0].bit_size})], [(${c[0].name})], fmul, mul)
[/][# th:each="c : ${float_builtins.fdiv}"]FLOAT_HELPER_2([(${c[0].bit_size})], [(${c[0].name})], fdiv, div)
[/][# th:each="c : ${float_builtins.fmadd}"]FLOAT_HELPER_3([(${c[0].bit_size})], [(${c[0].name})], fmadd, muladd, 0)
[/][# th:each="c : ${float_builtins.fmsub}"]FLOAT_HELPER_3([(${c[0].bit_size})], [(${c[0].name})], fmsub, muladd, float_muladd_negate_c)
[/][# th:each="c : ${float_builtins.fnmadd}"]FLOAT_HELPER_3([(${c[0].bit_size})], [(${c[0].name})], fnmadd, muladd, float_muladd_negate_c | float_muladd_negate_product)
[/][# th:each="c : ${float_builtins.fnmsub}"]FLOAT_HELPER_3([(${c[0].bit_size})], [(${c[0].name})], fnmsub, muladd, float_muladd_negate_product)
[/][# th:each="c : ${float_builtins.fmin}"]FLOAT_HELPER_MINMAX([(${c[0].bit_size})], [(${c[0].name})], fmin, minimum_number)
[/][# th:each="c : ${float_builtins.fmax}"]FLOAT_HELPER_MINMAX([(${c[0].bit_size})], [(${c[0].name})], fmax, maximum_number)
[/][# th:each="c : ${float_builtins.flt}"]FLOAT_HELPER_CMP([(${c[0].bit_size})], [(${c[0].name})], flt, lt)
[/][# th:each="c : ${float_builtins.fle}"]FLOAT_HELPER_CMP([(${c[0].bit_size})], [(${c[0].name})], fle, le)
[/][# th:each="c : ${float_builtins.feq}"]FLOAT_HELPER_CMP([(${c[0].bit_size})], [(${c[0].name})], feq, eq_quiet)
[/][# th:each="c : ${float_builtins.fcvt}"]FLOAT_HELPER_F2F([(${c[0].bit_size})], [(${c[0].name})], [(${c[1].bit_size})], [(${c[1].name})], fcvt)
[/][# th:each="c : ${float_builtins.fcvtfs}"]FLOAT_HELPER_F2I([(${c[0].bit_size})], [(${c[0].name})], [(${c[1]})], int[(${c[1]})], fcvtfs)
[/][# th:each="c : ${float_builtins.fcvtfu}"]FLOAT_HELPER_F2I([(${c[0].bit_size})], [(${c[0].name})], [(${c[1]})], uint[(${c[1]})], fcvtfu)
[/][# th:each="c : ${float_builtins.fcvtsf}"]FLOAT_HELPER_I2F([(${c[0].bit_size})], [(${c[0].name})], [(${c[1]})], int[(${c[1]})], fcvtsf)
[/][# th:each="c : ${float_builtins.fcvtuf}"]FLOAT_HELPER_I2F([(${c[0].bit_size})], [(${c[0].name})], [(${c[1]})], uint[(${c[1]})], fcvtuf)
[/][# th:each="c : ${float_builtins.fisinf}"]FLOAT_HELPER_CLASS([(${c[0].bit_size})], [(${c[0].name})], fisinf, is_infinity)
[/][# th:each="c : ${float_builtins.fiszero}"]FLOAT_HELPER_CLASS([(${c[0].bit_size})], [(${c[0].name})], fiszero, is_zero)
[/][# th:each="c : ${float_builtins.fisneg}"]FLOAT_HELPER_CLASS([(${c[0].bit_size})], [(${c[0].name})], fisneg, is_neg)
[/][# th:each="c : ${float_builtins.fisdenorm}"]FLOAT_HELPER_CLASS([(${c[0].bit_size})], [(${c[0].name})], fisdenorm, is_denormal) // TODO: less efficient than is_zero_or_denormal
[/][# th:each="c : ${float_builtins.fissnan}"]FLOAT_HELPER_CLASSS([(${c[0].bit_size})], [(${c[0].name})], fissnan, is_signaling_nan)
[/][# th:each="c : ${float_builtins.fisqnan}"]FLOAT_HELPER_CLASSS([(${c[0].bit_size})], [(${c[0].name})], fisqnan, is_quiet_nan)
[/]
