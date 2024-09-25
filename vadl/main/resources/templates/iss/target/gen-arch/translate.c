#include "qemu/osdep.h"
#include "exec/translator.h"
#include "qemu/qemu-print.h"
#include "tcg/tcg-op.h"

void [(${gen_arch_lower})]_tcg_init(void)
{
    qemu_printf("[VADL] TODO: [(${gen_arch_lower})]_tcg_init\n");
    // TODO: Later
}


/* entry point of code generation */
void gen_intermediate_code(CPUState *cs, TranslationBlock *tb, int *max_insns,
                           vaddr pc, void *host_pc) {

    qemu_printf("[[(${gen_arch_upper})]] gen_intermediate_code\n");
}