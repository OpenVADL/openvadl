/*
 * linux-user cpu loop for [(${config.archName})]
 *
 * This file implements the main execution loop for linux-user mode.
 */

#include "qemu/osdep.h"
#include "../../target/[(${config.archName})]/cpu.h" 
#include "../qemu.h"
#include "user-internals.h"
#include "cpu_loop-common.h"
#include "signal-common.h"
#include "exec/exec-all.h"

void cpu_loop(CPUArchState *env)
{
    CPUState *cs = env_cpu(env);
    int trapnr;
    abi_long ret;

    for (;;) {
        cpu_exec_start(cs);
        trapnr = cpu_exec(cs);
        cpu_exec_end(cs);

        process_queued_cpu_work(cs);

        switch (trapnr) {

        case EXCP_SYSCALL:
            ret = do_syscall(env,
                             env->gpr[[(${config.syscallRegister})]],   
                             env->gpr[[(${config.argRegisters[0]})]],   
                             env->gpr[[(${config.argRegisters[1]})]],   
                             env->gpr[[(${config.argRegisters[2]})]],   
                             env->gpr[[(${config.argRegisters[3]})]],  
                             env->gpr[[(${config.argRegisters[4]})]],   
                             env->gpr[[(${config.argRegisters[5]})]],   
                             0, 0);

            [# th:if="${config.errorConvention == 'negative_return'}"]
            if (ret == -TARGET_ERESTARTSYS) {
                env->pc -= [(${config.instructionSize})]; 
            } else if (ret != -TARGET_QEMU_ESIGRETURN) {
                env->gpr[[(${config.returnRegister})]] = ret;
            }
            [/]
            
            [# th:if="${config.errorConvention == 'flag_set'}"]
            // add bsd error convention here
            [/]
            
            env->pc += [(${config.instructionSize})]; 
            break;

        case EXCP_INTERRUPT:
            process_pending_signals(env);
            break;

        case EXCP_DEBUG:
            force_sig_fault(TARGET_SIGTRAP, TARGET_TRAP_BRKPT, env->pc);
            break;

        case EXCP_ATOMIC:
            cpu_exec_step_atomic(cs);
            break;

        default:
            fprintf(stderr, "Unhandled trap: 0x%x at pc=0x%lx\n", 
                    trapnr, (unsigned long)env->pc);
            force_sig_fault(TARGET_SIGILL, TARGET_ILL_ILLOPC,
                            env->pc);
            break;
        }
    }
}