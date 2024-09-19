#include "qemu/osdep.h"
#include "exec/translator.h"
#include "qemu/qemu-print.h"

/* entry point of code generation */
void gen_intermediate_code(CPUState *cs, TranslationBlock *tb, int *max_insns,
                           vaddr pc, void *host_pc) {

    qemu_printf("[[(${gen_arch_upper})]] gen_intermediate_code\n");
}