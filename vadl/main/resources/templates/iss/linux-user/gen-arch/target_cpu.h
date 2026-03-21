#ifndef [(${gen_arch_upper})]_TARGET_CPU_H
#define [(${gen_arch_upper})]_TARGET_CPU_H

//TODO: check for correct interpolattions for regs & values
enum {
    [(${gen_arch_upper})]_REG_RA = 1,
    [(${gen_arch_upper})]_REG_SP = 2,
    [(${gen_arch_upper})]_REG_TP = 4,
    [(${gen_arch_upper})]_REG_A0 = 10,
    [(${gen_arch_upper})]_REG_A1 = 11,
    [(${gen_arch_upper})]_REG_A2 = 12,
    [(${gen_arch_upper})]_REG_A3 = 13,
    [(${gen_arch_upper})]_REG_A4 = 14,
    [(${gen_arch_upper})]_REG_A5 = 15,
    [(${gen_arch_upper})]_REG_A7 = 17,
};

static inline void cpu_clone_regs_child(CPU[(${gen_arch_upper})]State *env, target_ulong newsp,
                                        unsigned flags)
{
    if (newsp) {
        env->x[[(${gen_arch_upper})]_REG_SP] = newsp;
    }

    env->x[[(${gen_arch_upper})]_REG_A0] = 0;
}

static inline void cpu_clone_regs_parent(CPU[(${gen_arch_upper})]State *env, unsigned flags)
{
}

static inline void cpu_set_tls(CPU[(${gen_arch_upper})]State *env, target_ulong newtls)
{
    env->x[[(${gen_arch_upper})]_REG_TP] = newtls;
}

static inline abi_ulong get_sp_from_cpustate(CPU[(${gen_arch_upper})]State *state)
{
   return state->x[[(${gen_arch_upper})]_REG_SP];
}
#endif
