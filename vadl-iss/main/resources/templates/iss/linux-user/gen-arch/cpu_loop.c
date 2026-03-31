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

enum {
    RV64UME_EXC_ILLEGAL_INSTR = 2,
    RV64UME_EXC_BREAKPOINT = 3,
    RV64UME_EXC_ECALL = 11,
};

void cpu_loop(CPURV64UMEState *env)
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
        case RV64UME_EXCP_EXC:
            cause = env->arg_exc_cause;
            switch (cause) {
            case RV64UME_EXC_ECALL:
                env->pc += 4;
                if (env->x[RV64UME_REG_A7] == TARGET_NR_rv64ume_flush_icache) {
                    /* no-op in QEMU; TB invalidation is automatic */
                    ret = 0;
                } else {
                    ret = do_syscall(env,
                                     env->x[RV64UME_REG_A7],
                                     env->x[RV64UME_REG_A0],
                                     env->x[RV64UME_REG_A1],
                                     env->x[RV64UME_REG_A2],
                                     env->x[RV64UME_REG_A3],
                                     env->x[RV64UME_REG_A4],
                                     env->x[RV64UME_REG_A5],
                                     0, 0);
                }
                if (ret == -QEMU_ERESTARTSYS) {
                    env->pc -= 4;
                } else if (ret != -QEMU_ESIGRETURN) {
                    env->x[RV64UME_REG_A0] = ret;
                }
                if (cs->singlestep_enabled) {
                    goto gdbstep;
                }
                break;
            case RV64UME_EXC_ILLEGAL_INSTR:
                force_sig_fault(TARGET_SIGILL, TARGET_ILL_ILLOPC, env->pc);
                break;
            case RV64UME_EXC_BREAKPOINT:
            case EXCP_DEBUG:
gdbstep:
                force_sig_fault(TARGET_SIGTRAP, TARGET_TRAP_BRKPT, env->pc);
                break;
            default:
                EXCP_DUMP(env,
                          "\nqemu: unhandled rv64ume exception cause %#x - aborting\n",
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

    env->pc = regs->sepc;
    env->x[RV64UME_REG_SP] = regs->sp;

    ts->stack_base = info->start_stack;
}
