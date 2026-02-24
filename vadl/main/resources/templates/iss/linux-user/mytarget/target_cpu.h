#ifndef MYTARGET_TARGET_CPU_H
#define MYTARGET_TARGET_CPU_H

#include "target/mytarget/cpu.h"

static inline void cpu_clone_regs_child(CPUArchState *env, target_ulong newsp,
                                        unsigned flags)
{
    if (newsp) {
        env->regs[2] = newsp;
    }

    env->regs[0] = 0;
}

static inline void cpu_clone_regs_parent(CPUArchState *env, unsigned flags)
{
}


static inline void cpu_set_tls(CPUArchState *env, target_ulong newtls)
{
    env->regs[13] = newtls;
}

static inline abi_ulong get_sp_from_cpustate(CPUArchState *env)
{
    return env->regs[2];
}

#endif