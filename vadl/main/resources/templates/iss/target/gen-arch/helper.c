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

void prep_float_status_fe_flags(CPU[(${gen_arch_upper})]State *env, float_status *s) {
  uint16_t flags = 0xffff;
  // un-set non sticky flags
  [# th:each="reg : ${register_tensors}"][# th:each="flag : ${reg.non_sticky_fe_flags}"]
  flags &= ~(1 << [(${flag.flag_idx})]);[/][/]
  // un-set sticky flags that are not set
  [# th:each="reg : ${register_tensors}"][# th:each="flag : ${reg.sticky_fe_flags}"]
  flags &= ~((1 - ((env->[(${reg.name_lower})] >> [(${flag.idx})]) & 1)) << [(${flag.flag_idx})]);[/][/]
  set_float_exception_flags(flags, s);
}

void set_float_status_fe_flags(CPU[(${gen_arch_upper})]State *env, float_status *s) {
  // DEV NOTE: for now we write directly to the flags register. This means that the helper is not pure and
  //           thus slower. In the future, we should optimize this.
  uint16_t flags = get_float_exception_flags(s);
  [# th:each="reg : ${register_tensors}"][# th:each="flag : ${reg.sticky_fe_flags}"]
  env->[(${reg.name_lower})] |= (flags >> [(${flag.flag_idx})] & 1) << [(${flag.idx})];[/][/]
  [# th:each="reg : ${register_tensors}"][# th:each="flag : ${reg.non_sticky_fe_flags}"]
  env->[(${reg.name_lower})] &= ~((1 - (flags >> [(${flag.flag_idx})] & 1)) << [(${flag.idx})]);[/][/]
}

#define FLOAT_FN_IEEE_FE_HELPER(S) \
  typedef uint##S##_t (*f##S##_fn_1)(uint##S##_t rs1, float_status *s);                     \
  typedef uint##S##_t (*f##S##_fn_2)(uint##S##_t rs1, uint##S##_t rs2, float_status *s);    \
  typedef uint##S##_t (*f##S##_fn_3)(uint##S##_t rs1, uint##S##_t rs2, uint##S##_t rs3,     \
                                     int flags, float_status *s);                           \
  uint##S##_t f##S##_fn_1_with_fe_flags(CPU[(${gen_arch_upper})]State *env,                 \
                                      f##S##_fn_1 fn, float_status *s,                      \
                                      uint##S##_t rs1) {                                    \
    prep_float_status_fe_flags(env, s);                                                     \
    uint##S##_t result = fn(rs1, s);                                                        \
    set_float_status_fe_flags(env, s);                                                      \
    return result;                                                                          \
  }                                                                                         \
  uint##S##_t f##S##_fn_2_with_fe_flags(CPU[(${gen_arch_upper})]State *env,                 \
                                      f##S##_fn_2 fn, float_status *s,                      \
                                      uint##S##_t rs1, uint##S##_t rs2) {                   \
    prep_float_status_fe_flags(env, s);                                                     \
    uint##S##_t result = fn(rs1, rs2, s);                                                   \
    set_float_status_fe_flags(env, s);                                                      \
    return result;                                                                          \
  }                                                                                         \
  uint##S##_t f##S##_fn_3_with_fe_flags(CPU[(${gen_arch_upper})]State *env,                 \
                                      f##S##_fn_3 fn, float_status *s, int flags,           \
                                      uint##S##_t rs1, uint##S##_t rs2, uint##S##_t rs3) {  \
    prep_float_status_fe_flags(env, s);                                                     \
    uint##S##_t result = fn(rs1, rs2, rs3, flags, s);                                       \
    set_float_status_fe_flags(env, s);                                                      \
    return result;                                                                          \
  }

#define FLOAT_HELPER_1(S, FMT, NAME, QEMU_FUN) \
  uint##S##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,  \
                                   uint##S##_t rs1) {                    \
    return f64_fn_1_with_fe_flags(env, float##S##_##QEMU_FUN,            \
                                   &env->fp_status_##FMT, rs1);          \
  }

#define FLOAT_HELPER_2(S, FMT, NAME, QEMU_FUN) \
  uint##S##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,  \
                                   uint##S##_t rs1, uint##S##_t rs2) {   \
    return f64_fn_2_with_fe_flags(env, float##S##_##QEMU_FUN,            \
                                   &env->fp_status_##FMT, rs1, rs2);     \
  }

#define FLOAT_HELPER_3(S, FMT, NAME, QEMU_FUN, FLAGS) \
  uint##S##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,                  \
                                   uint##S##_t rs1, uint##S##_t rs2, uint##S##_t rs3) {  \
    return f64_fn_3_with_fe_flags(env, float##S##_##QEMU_FUN,                            \
                                   &env->fp_status_##FMT, FLAGS, rs1, rs2, rs3);         \
  }

#define FLOAT_HELPER_F2I(S, FMT, INT_FMT, NAME) \
  INT_FMT##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,  \
                                   uint##S##_t rs1) {                    \
    return f64_fn_1_with_fe_flags(env, float##S##_to_##INT_FMT,          \
                                   &env->fp_status_##FMT, rs1);          \
  }

#define FLOAT_HELPER_I2F(S, FMT, INT_FMT, NAME) \
  uint##S##_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,  \
                                   INT_FMT##_t rs1) {                    \
    return f64_fn_1_with_fe_flags(env, INT_FMT##_to_##float##S,          \
                                   &env->fp_status_##FMT, rs1);          \
  }

#define FLOAT_HELPER_F2F(S, S2, FMT, FMT2, NAME) \
  uint##S2##_t helper_##FMT##_##FMT2##_##NAME(CPU[(${gen_arch_upper})]State *env,  \
                                   uint##S##_t rs1) {                              \
    return f64_fn_1_with_fe_flags(env, float##S##_to_##float##S2,                  \
                                   &env->fp_status_##FMT, rs1);                    \
  }

// TODO: optimize fe flags (maybe prep can be omitted; or flags set to avoid recomputation)
#define FLOAT_HELPER_CMP(S, FMT, NAME, QEMU_FUN) \
  uint64_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,     \
                                   uint##S##_t rs1, uint##S##_t rs2) {   \
    return f64_fn_2_with_fe_flags(env, float##S##_##QEMU_FUN,            \
                                   &env->fp_status_##FMT, rs1, rs2);     \
  }

#define FLOAT_HELPER_CLASS(S, FMT, NAME, QEMU_FUN) \
  uint64_t helper_##FMT##_##NAME(CPU[(${gen_arch_upper})]State *env,     \
                                   uint##S##_t rs1) {                    \
    return f64_fn_1_with_fe_flags(env, float##S##_##QEMU_FUN,            \
                                   &env->fp_status_##FMT, rs1);          \
  }

// just use uint64_t for everything for now to keep things simple
// the actual helper call signatures do contain the right sizes (and all unsigned)
FLOAT_FN_IEEE_FE_HELPER(64)

[# th:each="fmt : ${float_formats}"]
FLOAT_HELPER_1([(${fmt.bit_size})], [(${fmt.name})], fsqrt, sqrt)

FLOAT_HELPER_2([(${fmt.bit_size})], [(${fmt.name})], fadd, add)
FLOAT_HELPER_2([(${fmt.bit_size})], [(${fmt.name})], fsub, sub)
FLOAT_HELPER_2([(${fmt.bit_size})], [(${fmt.name})], fmul, mul)
FLOAT_HELPER_2([(${fmt.bit_size})], [(${fmt.name})], fdiv, div)

FLOAT_HELPER_3([(${fmt.bit_size})], [(${fmt.name})], fmadd, muladd, 0)
FLOAT_HELPER_3([(${fmt.bit_size})], [(${fmt.name})], fmsub, muladd, float_muladd_negate_c)
FLOAT_HELPER_3([(${fmt.bit_size})], [(${fmt.name})], fnmadd, muladd, float_muladd_negate_c | float_muladd_negate_product)
FLOAT_HELPER_3([(${fmt.bit_size})], [(${fmt.name})], fnmsub, muladd, float_muladd_negate_product)

FLOAT_HELPER_2([(${fmt.bit_size})], [(${fmt.name})], fmin, minimum_number)
FLOAT_HELPER_2([(${fmt.bit_size})], [(${fmt.name})], fmax, maximum_number)

// TODO: risc-v specifies eq as quiet. other ISAs might want to configure this
FLOAT_HELPER_CMP([(${fmt.bit_size})], [(${fmt.name})], flt, lt)
FLOAT_HELPER_CMP([(${fmt.bit_size})], [(${fmt.name})], fle, le)
FLOAT_HELPER_CMP([(${fmt.bit_size})], [(${fmt.name})], feq, eq_quiet)

FLOAT_HELPER_F2I([(${fmt.bit_size})], [(${fmt.name})], uint32, fcvtfss)
FLOAT_HELPER_F2I([(${fmt.bit_size})], [(${fmt.name})], uint64, fcvtfsd)
FLOAT_HELPER_F2I([(${fmt.bit_size})], [(${fmt.name})], uint32, fcvtfus)
FLOAT_HELPER_F2I([(${fmt.bit_size})], [(${fmt.name})], uint64, fcvtfud)

// FIXME: this is currently very complex and will change
[# th:if="${fmt.bit_size != 64}"]
FLOAT_HELPER_I2F(32, [(${fmt.name})], uint32, fcvtssf)
FLOAT_HELPER_I2F(32, [(${fmt.name})], uint64, fcvtsdf)
FLOAT_HELPER_I2F(32, [(${fmt.name})], uint32, fcvtusf)
FLOAT_HELPER_I2F(32, [(${fmt.name})], uint64, fcvtudf)
[/]
[# th:if="${fmt.bit_size != 32}"]
FLOAT_HELPER_I2F(64, [(${fmt.name})], uint32, fcvtssf2)
FLOAT_HELPER_I2F(64, [(${fmt.name})], uint64, fcvtsdf2)
FLOAT_HELPER_I2F(64, [(${fmt.name})], uint32, fcvtusf2)
FLOAT_HELPER_I2F(64, [(${fmt.name})], uint64, fcvtudf2)
[/]
[# th:each="fmt2 : ${float_formats}"][# th:if="${fmt.name != fmt2.name}"]
[# th:if="${fmt.bit_size != 32}"]FLOAT_HELPER_F2F([(${fmt.bit_size})], 32, [(${fmt.name})], [(${fmt2.name})], fcvtff)[/]
[# th:if="${fmt.bit_size != 64}"]FLOAT_HELPER_F2F([(${fmt.bit_size})], 64, [(${fmt.name})], [(${fmt2.name})], fcvtff2)[/]
[/][/]

FLOAT_HELPER_CLASS([(${fmt.bit_size})], [(${fmt.name})], fisinf, is_infinity)
FLOAT_HELPER_CLASS([(${fmt.bit_size})], [(${fmt.name})], fiszero, is_zero)
FLOAT_HELPER_CLASS([(${fmt.bit_size})], [(${fmt.name})], fisneg, is_neg)
FLOAT_HELPER_CLASS([(${fmt.bit_size})], [(${fmt.name})], fisdenorm, is_denormal) // TODO: less efficient than is_zero_or_denormal
FLOAT_HELPER_CLASS([(${fmt.bit_size})], [(${fmt.name})], fissnan, is_signaling_nan)
FLOAT_HELPER_CLASS([(${fmt.bit_size})], [(${fmt.name})], fisqnan, is_quiet_nan)
[/]
