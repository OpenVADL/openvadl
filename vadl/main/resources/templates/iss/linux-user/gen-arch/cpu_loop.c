/*
 *  qemu user cpu loop
 *
 *  Copyright (c) 2003-2008 Fabrice Bellard
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

#include "qemu/osdep.h"
#include "qemu.h"
#include "user-internals.h"
#include "cpu_loop-common.h"
#include "signal-common.h"
#include "elf.h"


//TODO check for diff way to get exc+value
enum {
    [# th:each="exc : ${config.excIds}"]
       [(${gen_arch_upper})]_EXC_[(${exc.key})] = [(${exc.value})],
   [/]
};

void cpu_loop(CPU[(${gen_arch_upper})]State *env)
{
    CPUState *cs = env_cpu(env);
    int trapnr;
    uint32_t cause;
    target_ulong ret;

    for (;;) {
        cpu_exec_start(cs);
        trapnr = cpu_exec(cs);
        cpu_exec_end(cs);
        process_queued_cpu_work(cs);

        switch (trapnr) {
        case EXCP_INTERRUPT:
            /* just indicate that signals should be handled asap */
            break;
        case EXCP_ATOMIC:
            cpu_exec_step_atomic(cs);
            break;
        case [(${gen_arch_upper})]_EXCP_EXC:
            cause = env->[(${config.excCauseVar})];
            switch (cause) {
            case [(${gen_arch_upper})]_EXC_[(${config.SYSCALL_NAME})]:
                env->[(${pc_reg.name_lower})] += [(${config.insn_width_bytes})];
                [# th:if="${config.hasIcacheFlush}"]
                if (env->[(${register_tensors[0].name_lower})][ [(${config.sysReg})] ] == TARGET_NR_[(${gen_arch_lower})]_flush_icache) {
                    /* no-op in QEMU; TB invalidation is automatic */
                    ret = 0;
                } else {
                [/]
                    ret = do_syscall(env,
                                      env->[(${register_tensors[0].name_lower})][ [(${config.sysReg})] ],
                                                       [(${config.args.size() > 0 ? 'env->' + register_tensors[0].name_lower + '[' + config.args.get(0) + ']' : '0'})],
                                                       [(${config.args.size() > 1 ? 'env->' + register_tensors[0].name_lower + '[' + config.args.get(1) + ']' : '0'})],
                                                       [(${config.args.size() > 2 ? 'env->' + register_tensors[0].name_lower + '[' + config.args.get(2) + ']' : '0'})],
                                                       [(${config.args.size() > 3 ? 'env->' + register_tensors[0].name_lower + '[' + config.args.get(3) + ']' : '0'})],
                                                       [(${config.args.size() > 4 ? 'env->' + register_tensors[0].name_lower + '[' + config.args.get(4) + ']' : '0'})],
                                                       [(${config.args.size() > 5 ? 'env->' + register_tensors[0].name_lower + '[' + config.args.get(5) + ']' : '0'})],
                                                       0, 0);
                [# th:if="${config.hasIcacheFlush}"]
                }
                [/]
                if (ret == -QEMU_ERESTARTSYS) {
                    env->[(${pc_reg.name_lower})] -= [(${config.insn_width_bytes})];
                } else if (ret != -QEMU_ESIGRETURN) {
                    env->[(${register_tensors[0].name_lower})][ [(${config.retReg})] ] = ret;                }
                if (cs->singlestep_enabled) {
                    goto gdbstep;
                }
                break;
            case [(${gen_arch_upper})]_EXC_[(${config.ILLEGAL_INSTR_NAME})]:
                force_sig_fault(TARGET_SIGILL, TARGET_ILL_ILLOPC, env->[(${pc_reg.name_lower})]);
                break;
            case [(${gen_arch_upper})]_EXC_[(${config.BREAKPOINT_NAME})]:
            case EXCP_DEBUG:
            gdbstep:
                force_sig_fault(TARGET_SIGTRAP, TARGET_TRAP_BRKPT, env->[(${pc_reg.name_lower})]);
                break;
            default:
                EXCP_DUMP(env,
                          "\nqemu: unhandled [(${gen_arch_lower})] exception cause %#x - aborting\n",
                          "\nqemu: unhandled [(${gen_arch_lower})] exception cause %#x - aborting\n",
                          cause);
                exit(EXIT_FAILURE);
            }
            break;
        default:
            EXCP_DUMP(env, "\nqemu: unhandled CPU exception %#x - aborting\n",
                     trapnr);
            exit(EXIT_FAILURE);
        }

        process_pending_signals(env);
    }
}

void target_cpu_copy_regs(CPUArchState *env, struct target_pt_regs *regs)
{
    CPUState *cpu = env_cpu(env);
    TaskState *ts = get_task_state(cpu);
    struct image_info *info = ts->info;

    env->[(${pc_reg.name_lower})] = regs->[(${config.ptRegPc})];
    env->[(${register_tensors[0].name_lower})][ [(${config.spReg})] ] = regs->[(${config.ptRegSp})];

    ts->stack_base = info->start_stack;
}
