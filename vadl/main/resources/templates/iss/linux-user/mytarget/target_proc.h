/*
 * MYTARGET specific proc functions for linux-user
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
#ifndef MYTARGET_TARGET_PROC_H
#define MYTARGET_TARGET_PROC_H

static int open_cpuinfo(CPUArchState *cpu_env, int fd)
{
    int i;
    int num_cpus = sysconf(_SC_NPROCESSORS_ONLN);
    
    /* * don't have 'RISCVCPU' or 'isa_string' functions yet
     * So define static strings for now
     */

    for (i = 0; i < num_cpus; i++) {
        dprintf(fd, "processor\t: %d\n", i);
        dprintf(fd, "cpu model\t: mytarget-v1\n");
        
        /* * RISC-V prints an 'isa' string here (e.g., rv64imafdc).
         * print architecture name.
         */
        dprintf(fd, "arch\t\t: mytarget\n");
        
        //RISC-V specific MMU info. 
        dprintf(fd, "mmu\t\t: standard\n");
        
        dprintf(fd, "uarch\t\t: qemu\n\n");
    }

    return 0;
}
#define HAVE_ARCH_PROC_CPUINFO

#endif /* MYTARGET_TARGET_PROC_H */
