
#include "qemu/osdep.h"
#include "gdbstub/helpers.h"
#include "cpu.h"

int [(${gen_arch_lower})]_cpu_gdb_read_register(CPUState *cs, GByteArray *mem_buf, int n) {
    CPU[(${gen_arch_upper})]State *env = cpu_env(cs);

[(${read_regs})]
}

int [(${gen_arch_lower})]_cpu_gdb_write_register(CPUState *cs, uint8_t *mem_buf, int n) {
    CPU[(${gen_arch_upper})]State *env = cpu_env(cs);

[(${write_regs})]
}