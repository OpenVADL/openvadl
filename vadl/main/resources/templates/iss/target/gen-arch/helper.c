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
  typedef uint##S##_t (*f##S##_fn)(uint##S##_t a, uint##S##_t b, float_status *s);  \
  uint##S##_t f##S##_fn_with_fe_flags(CPU[(${gen_arch_upper})]State *env,           \
                                      f##S##_fn fn, float_status *s,                \
                                      uint##S##_t rs1, uint##S##_t rs2) {           \
    prep_float_status_fe_flags(env, s);                                             \
    uint##S##_t result = fn(rs1, rs2, s);                                           \
    set_float_status_fe_flags(env, s);                                              \
    return result;                                                                  \
  }

#define FLOAT_HELPER_2(S, NAME, FUN) \
  uint##S##_t helper_fadd_##NAME(CPU[(${gen_arch_upper})]State *env,    \
                                   uint##S##_t rs1, uint##S##_t rs2) {  \
    return f##S##_fn_with_fe_flags(env, float##S##_##FUN,               \
                                   &env->fp_status_##NAME, rs1, rs2);   \
  }

[# th:each="size : ${float_ieee_sizes}"]
FLOAT_FN_IEEE_FE_HELPER([(${size})])[/]

[# th:each="fmt : ${float_formats}"]
FLOAT_HELPER_2([(${fmt.bit_size})], [(${fmt.name})], add)[/]
