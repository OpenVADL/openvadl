
#include "qemu/osdep.h"
#include "cpu.h"
#include "exec/exec-all.h"
#include "exec/cpu_ldst.h"
#include "exec/helper-proto.h"
#include "qemu/log-for-trace.h"

void helper_unsupported(CPUVADLState *env) {

    CPUState *cs = env_cpu(env);

    cs->exception_index = EXCP_HLT;

    qemu_log("Unsupported instruction at %08llx\n", env->pc);
    cpu_loop_exit(cs);
}