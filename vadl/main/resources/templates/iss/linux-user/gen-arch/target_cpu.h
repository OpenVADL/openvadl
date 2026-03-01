#ifndef RV64UME_TARGET_CPU_H
#define RV64UME_TARGET_CPU_H

enum {
    RV64UME_REG_RA = 1,
    RV64UME_REG_SP = 2,
    RV64UME_REG_TP = 4,
    RV64UME_REG_A0 = 10,
    RV64UME_REG_A1 = 11,
    RV64UME_REG_A2 = 12,
    RV64UME_REG_A3 = 13,
    RV64UME_REG_A4 = 14,
    RV64UME_REG_A5 = 15,
    RV64UME_REG_A7 = 17,
};

static inline void cpu_clone_regs_child(CPURV64UMEState *env, target_ulong newsp,
                                        unsigned flags)
{
    if (newsp) {
        env->x[RV64UME_REG_SP] = newsp;
    }

    env->x[RV64UME_REG_A0] = 0;
}

static inline void cpu_clone_regs_parent(CPURV64UMEState *env, unsigned flags)
{
}

static inline void cpu_set_tls(CPURV64UMEState *env, target_ulong newtls)
{
    env->x[RV64UME_REG_TP] = newtls;
}

static inline abi_ulong get_sp_from_cpustate(CPURV64UMEState *state)
{
   return state->x[RV64UME_REG_SP];
}
#endif
