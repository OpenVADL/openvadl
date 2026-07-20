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
  // DEV NOTE: this is generated from float flag annotations
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

[# th:each="size : ${float_ieee_sizes}"]
typedef uint[(${size})]_t (*f[(${size})]_fn)(uint[(${size})]_t a, uint[(${size})]_t b, float_status *s);
uint[(${size})]_t f[(${size})]_fn_with_fe_flags(CPU[(${gen_arch_upper})]State *env, f[(${size})]_fn fn, float_status *s, uint[(${size})]_t rs1, uint[(${size})]_t rs2) {
  // DEV NOTE: flag stuff can be simplified when no flags present. But is that necessary? The flag functions
  //           should set all flags, if none are present in the spec.
  // DEV NOTE: lets have a separate float_status for each float-type in the spec. but some things (e.g. rounding mode)
  //           are specified per call, so the float_status will be modified here either way (also because of flags).
  //           We could scratch that and rebuild a float_status from scratch every time -> env not affected.
  prep_float_status_fe_flags(env, s);
  uint[(${size})]_t result = fn(rs1, rs2, s);
  set_float_status_fe_flags(env, s);
  return result;
}
[/]

[# th:each="fmt : ${float_formats}"]
uint[(${fmt.bit_size})]_t helper_fadd_[(${fmt.name})](CPU[(${gen_arch_upper})]State *env, uint[(${fmt.bit_size})]_t rs1, uint[(${fmt.bit_size})]_t rs2) {
  // TODO: float[(${fmt.bit_size})]_add can only be used for ieee formats. other formats will need other functions
  return f[(${fmt.bit_size})]_fn_with_fe_flags(env, float[(${fmt.bit_size})]_add, &env->fp_status_[(${fmt.name})], rs1, rs2);
}
[/]
