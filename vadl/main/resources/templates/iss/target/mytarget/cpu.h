#ifndef MYTARGET_CPU_H
#define MYTARGET_CPU_H

#include "qemu/osdep.h"
#include "cpu-param.h"
#include "exec/cpu-defs.h"
#include "hw/core/cpu.h"
#include "qom/object.h"

typedef struct CPUArchState {
    uint64_t regs[32];
    uint64_t pc;
    uint64_t flags;
    uint32_t misa_ext;
} CPUArchState;

#include "cpu-qom.h"

#define TCG_GUEST_DEFAULT_MO TCG_MO_ALL
#define CPU_RESOLVING_TYPE TYPE_MYTARGET_CPU
#define TARGET_ERESTARTSYS     512
#define TARGET_QEMU_ESIGRETURN 513

#define EXCP_SYSCALL    0x100

#define cpu_signal_handler cpu_mytarget_signal_handler

static inline void cpu_get_tb_cpu_state(CPUArchState *env, target_ulong *pc,
                                        target_ulong *cs_base, uint32_t *flags)
{
    *pc = env->pc;
    *cs_base = 0;
    *flags = 0;
}

#define cpu_list mytarget_cpu_list
void mytarget_cpu_list(void);

#include "exec/cpu-all.h"

#endif