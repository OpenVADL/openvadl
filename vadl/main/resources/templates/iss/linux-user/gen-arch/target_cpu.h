#ifndef [(${gen_arch_upper})]_TARGET_CPU_H
#define [(${gen_arch_upper})]_TARGET_CPU_H

//TODO: check for correct interpolations for regs & values
enum {
    [(${gen_arch_upper})]_REG_RA  = 1,
    [(${gen_arch_upper})]_REG_SP  = [(${config.spReg})],
    [(${gen_arch_upper})]_REG_TP  = 4,

    [# th:each="arg, stat : ${config.args}"]
    [(${gen_arch_upper})]_REG_ARG[(${stat.index})] = [(${arg.index})],
    [/]
};

static inline void cpu_clone_regs_child(CPU[(${gen_arch_upper})]State *env, target_ulong newsp,
                                        unsigned flags)
{
    if (newsp) {
        env->[(${register_tensors[0].name_lower})][ [(${gen_arch_upper})]_REG_SP] = newsp;
    }

    env->[(${register_tensors[0].name_lower})][ [(${config.retReg})] ] = 0;
}

static inline void cpu_clone_regs_parent(CPU[(${gen_arch_upper})]State *env, unsigned flags)
{
}

static inline void cpu_set_tls(CPU[(${gen_arch_upper})]State *env, target_ulong newtls)
{
    env->[(${register_tensors[0].name_lower})][ [(${gen_arch_upper})]_REG_TP] = newtls;
}

static inline abi_ulong get_sp_from_cpustate(CPU[(${gen_arch_upper})]State *state)
{
   return state->[(${register_tensors[0].name_lower})][ [(${gen_arch_upper})]_REG_SP];
}
#endif
