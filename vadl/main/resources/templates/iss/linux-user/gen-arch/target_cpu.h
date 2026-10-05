

#ifndef [(${gen_arch_upper})]_TARGET_CPU_H
#define [(${gen_arch_upper})]_TARGET_CPU_H

static inline void cpu_clone_regs_child(CPU[(${gen_arch_upper})]State *env, target_ulong newsp,
                                        unsigned flags)
{
    if (newsp) {
        env->[(${config.spReg})] = newsp;
    }

    env->[(${config.retReg})] = 0;
}

static inline void cpu_clone_regs_parent(CPU[(${gen_arch_upper})]State *env, unsigned flags)
{
}

[# th:if="${config.tpReg} != null"]
static inline void cpu_set_tls(CPU[(${gen_arch_upper})]State *env, target_ulong newtls)
{
    env->[(${config.tpReg})] = newtls;
}
[/]

static inline abi_ulong get_sp_from_cpustate(CPU[(${gen_arch_upper})]State *state)
{
   return state->[(${config.spReg})];
}
#endif