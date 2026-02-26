/*
 * RISC-V specific proc functions for linux-user
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
#ifndef RV64UME_TARGET_PROC_H
#define RV64UME_TARGET_PROC_H

static int open_cpuinfo(CPUArchState *cpu_env, int fd)
{
    (void)cpu_env;

    dprintf(fd, "processor\t: 0\n");
    dprintf(fd, "hart\t\t: 0\n");
    dprintf(fd, "isa\t\t: rv64im\n");
    dprintf(fd, "mmu\t\t: none\n");
    dprintf(fd, "uarch\t\t: qemu\n\n");

    return 0;
}
#define HAVE_ARCH_PROC_CPUINFO

#endif /* RV64UME_TARGET_PROC_H */
