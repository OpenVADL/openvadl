/*
 * RISC-V specific proc functions for linux-user
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
#ifndef [(${gen_arch_upper})]_TARGET_PROC_H
#define [(${gen_arch_upper})]_TARGET_PROC_H

static int open_cpuinfo(CPUArchState *cpu_env, int fd)
{
    (void)cpu_env;

    dprintf(fd, "processor\t: 0\n");
    dprintf(fd, "hart\t\t: 0\n");
    dprintf(fd, "isa\t\t: [(${gen_arch_lower})]\n");
    dprintf(fd, "mmu\t\t: none\n");
    dprintf(fd, "uarch\t\t: qemu\n\n");

    return 0;
}
#define HAVE_ARCH_PROC_CPUINFO

#endif /* [(${gen_arch_upper})]_TARGET_PROC_H */
