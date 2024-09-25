// [(${gen_arch_upper})] generated file
#include "qemu/osdep.h"
#include "tcg/tcg.h"
#include "exec/exec-all.h"
#include "exec/log.h"
#include "exec/translator.h"
#include "qemu/qemu-print.h"
#include "tcg/tcg-op.h"


static TCGv cpu_pc;
[# th:each="reg_file, iterState : ${register_files}"] // define the register file tcgs
static TCGv cpu_[(${reg_file.name_lower})][(${"[" + reg_file["size"] + "]"})];
[/]

typedef struct DisasContext {
  DisasContextBase base;

  CPU[(${gen_arch_upper})]State *env;
  [# th:each="reg_file, iterState : ${register_files}"] // constraint value constants
  [# th:each="constraint, iterState : ${reg_file.constraints}"]
  TCGv const[(${reg_file.name_lower})][(${constraint.value})];
  [/][/]

} DisasContext;


void [(${gen_arch_lower})]_tcg_init(void)
{
    int i;
    qemu_printf("[[(${gen_arch_upper})]] TODO: [(${gen_arch_lower})]_tcg_init\n");

    // set the cpu_pc TCGv
    cpu_pc = tcg_global_mem_new(tcg_env, offsetof(CPU[(${gen_arch_upper})]State, [(${gen_arch_upper})]_PC), "[(${gen_arch_upper})]_PC");
    [# th:each="reg_file, iterState : ${register_files}"]
    [# th:each="constraint, iterState : ${reg_file.constraints}"]
    // Register [(${reg_file.names[constraint.index]})] is placeholder for [(${constraint.value})]. DO NOT USE IT.
    // Use gen_set_[(${reg_file.name_lower})] and get_[(${reg_file.name_lower})] helper functions when accessing.
    cpu_[(${reg_file.name_lower})][(${"[" + constraint.index + "]"})] = NULL;
    [/]
    for (i = 0; i < [(${reg_file["size"]})]; i++) {
        [# th:each="constraint, iterState : ${reg_file.constraints}"]
        if (i == [(${constraint.index})]) continue; // Skip index as it should stay NULL;
        [/]
        cpu_[(${reg_file.name_lower})][i] = tcg_global_mem_new(tcg_env,
                                        offsetof(CPU[(${gen_arch_upper})]State, [(${reg_file.name_lower})][i]),
                                        [(${gen_arch_lower})]_cpu_[(${reg_file.name_lower})]_names[i]);
    }[/]
}

/*
 * Helper functions called during translation:
 *
 *    - next_instr() ... reads the next encoded instruction from pc
 *    - ex_shift_n() ... shifts an immediate by some amount
 *    - ex_plus_1()  ... adds one to given immediate
 *    - get_<reg_file>()     ... returns a TCGv (variable) for the given register
 *    - dest_<reg_file>()    ... returns a TCGv (variable) to store result in
 *    - gen_set_<reg_file>() ... generates a write of the given TCGv to the given register
 *    - gen_goto_tb()        ... generates a jump with a given diff
 *
 */

// TODO: Could potentially be removed
static int ex_plus_1(DisasContext *ctx, int nf)
{
    return nf + 1;
}

// TODO: Could potentially be removed
#define EX_SH(amount) \
static int ex_shift_##amount(DisasContext *ctx, int imm) \
{                                         \
    return imm << amount;                 \
}
EX_SH(1)
EX_SH(2)
EX_SH(3)
EX_SH(4)
EX_SH(12)

static target_ulong next_insn(DisasContext *ctx)
{
    vaddr  pc_next = ctx->base.pc_next;
    return translator_ld[(${insn_width.short})](ctx->env, &ctx->base, pc_next);
}


/*
 * Instruction translation functions.
 * Called by decode_insn() function produced by insn.deocde decode-tree.
 */

static bool decode_insn(DisasContext *ctx, uint[(${insn_width.int})]_t insn);
#include "decode-insn.c.inc"

/*
 *  Core translation mechanism functions:
 *
 *    - translate()
 *    - gen_intermediate_code()
 *
 */
static void translate(DisasContext *ctx)
{
    uint32_t insn = next_insn(ctx);
    if(!decode_insn(ctx, insn)) {
        error_report("[[(${gen_arch_upper})]] translate, illegal instr, pc: 0x%04llx , insn: 0x%04x\n", ctx->base.pc_next, insn);
        CPUState *cs = env_cpu(ctx->env);

        // TODO: produce exception
        assert(false);
    }
}

static void [(${gen_arch_lower})]_tr_init_disas_context(DisasContextBase *db, CPUState *cs)
{
    DisasContext *ctx = container_of(db, DisasContext, base);
    CPU[(${gen_arch_upper})]State *env = cpu_env(cs);
    [(${gen_arch_upper})]CPUClass *mcc = [(${gen_arch_upper})]_CPU_GET_CLASS(cs);
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);

    ctx->env = env;
    [# th:each="reg_file, iterState : ${register_files}"]
    [# th:each="constraint, iterState : ${reg_file.constraints}"]
    ctx->const[(${reg_file.name_lower})][(${constraint.value})] = tcg_constant_i[(${reg_file.value_width})]([(${constraint.value})]);
    [/][/]
}

static void [(${gen_arch_lower})]_tr_tb_start(DisasContextBase *db, CPUState *cpu)
{

}

static void [(${gen_arch_lower})]_tr_insn_start(DisasContextBase *db, CPUState *cpu)
{
    DisasContext *ctx = container_of(db, DisasContext, base);
    target_ulong  pc_next = ctx->base.pc_next;
    // TODO
    tcg_gen_insn_start(pc_next);
}

static void [(${gen_arch_lower})]_tr_translate_insn(DisasContextBase *db, CPUState *cpu)
{
    qemu_printf("[[(${gen_arch_upper})]] [(${gen_arch_lower})]_tr_translate_insn\n");
    DisasContext *ctx = container_of(db, DisasContext, base);

    // translate current insn
    translate(ctx);
    // increment program counter
    db->pc_next = db->pc_next + ([(${insn_width.int})] / [(${mem_word_size.int})]);
}

static void [(${gen_arch_lower})]_tr_tb_stop(DisasContextBase *db, CPUState *cpu)
{
    DisasContext *ctx = container_of(db, DisasContext, base);
    // TODO
}

static const TranslatorOps [(${gen_arch_lower})]_tr_ops = {
        .init_disas_context = [(${gen_arch_lower})]_tr_init_disas_context,
        .tb_start = [(${gen_arch_lower})]_tr_tb_start,
        .insn_start = [(${gen_arch_lower})]_tr_insn_start,
        .translate_insn = [(${gen_arch_lower})]_tr_translate_insn,
        .tb_stop = [(${gen_arch_lower})]_tr_tb_stop,
};

/* entry point of code generation */
void gen_intermediate_code(CPUState *cs, TranslationBlock *tb, int *max_insns,
                           vaddr pc, void *host_pc) {
    DisasContext ctx;
    translator_loop(cs, tb, max_insns, pc, host_pc, &vadl_tr_ops, &ctx.base);
}