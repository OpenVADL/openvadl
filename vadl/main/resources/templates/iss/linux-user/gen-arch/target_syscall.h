/*
 * This struct defines the way the registers are stored on the
 *  stack during a system call.
 *
 * Reference: linux/arch/[(${gen_arch_lower})]/include/uapi/asm/ptrace.h
 */

#ifndef LINUX_USER_[(${gen_arch_upper})]_TARGET_SYSCALL_H
#define LINUX_USER_[(${gen_arch_upper})]_TARGET_SYSCALL_H

struct target_pt_regs {
    abi_long sepc;
    abi_long pc;
    abi_long sp;
};

#define UNAME_MACHINE "[(${gen_arch_lower})]"
#define UNAME_MINIMUM_RELEASE "4.15.0"

#define TARGET_MCL_CURRENT 1
#define TARGET_MCL_FUTURE  2
#define TARGET_MCL_ONFAULT 4

/* clone(flags, newsp, ptidptr, tls, ctidptr) for RISC-V */
/* This comes from linux/kernel/fork.c, CONFIG_CLONE_BACKWARDS */
#define TARGET_CLONE_BACKWARDS

#endif
