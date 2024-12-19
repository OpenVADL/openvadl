#include "qemu/osdep.h"
#include "cpu.h"
#include "exec/exec-all.h"
#include "exec/cpu_ldst.h"
#include "exec/helper-proto.h"
#include "qemu/log-for-trace.h"
#include "qemu/qemu-print.h"
#include "cpu-bits.h"

G_NORETURN void [(${gen_arch_lower})]_raise_exception(CPU[(${gen_arch_upper})]State *env, int32_t exception, uintptr_t pc) {
    CPUState *cs = env_cpu(env);

    cs->exception_index = exception;
    cpu_loop_exit_restore(cs, pc);
}


void helper_raise_exception(CPU[(${gen_arch_upper})]State *env, uint32_t exception) {
    // Exit cpu loop without restore.
    // If this helper function is raised, it is also the end of the TB any ways, so no restore necessary.
    [(${gen_arch_lower})]_raise_exception(env, exception, 0);
}

void helper_unsupported(CPU[(${gen_arch_upper})]State *env) {
    CPUState *cs = env_cpu(env);

    cs->exception_index = EXCP_HLT;

    cpu_loop_exit(cs);
}


target_ulong helper_csrrw(CPU[(${gen_arch_upper})]State *env, int csr, target_ulong src) {
    // Currently the only CSR to access is MTVEC.
    if (csr != CSR_MTVEC) {
      qemu_printf("[VADL] CSR is not MTVEC, was %x . Do nothing.", csr);
    }

    const target_ulong val = env->mtvec;
    env->mtvec             = src;
    return val;
}
