// [(${gen_arch_upper})] generated file
#include "qemu/osdep.h"
#include "tcg/tcg.h"
#include "exec/exec-all.h"
#include "exec/log.h"
#include "exec/translator.h"
#include "qemu/qemu-print.h"
#include "tcg/tcg-op.h"
#include "cpu-bits.h"
#include "trace.h"
#include "vadl-builtins.h"

#include "exec/helper-proto.h"
#include "exec/helper-gen.h"
#define HELPER_H "helper.h"
#include "exec/helper-info.c.inc"
#undef  HELPER_H

static TCGv cpu_pc;

[# th:if="${insn_count}"]
static TCGv cpu_insn_count;
[/]

[# th:each="reg_file, iterState : ${register_files}"] // define the register file tcgs
static TCGv cpu_[(${reg_file.name_lower})][(${"[" + reg_file["size"] + "]"})];
[/]

/* We have a single condition exit.
   So reaching the end of the branch instruction means, we want to execute the
   following instruction as well -> we want to chain the default (no taking) branch with
   the next instruction. This is handled in the tb_stop function by calling goto_tb with
   the next instruction PC.
 */
#define DISAS_CHAIN  DISAS_TARGET_0

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

    // set the cpu_pc TCGv
    cpu_pc         = tcg_global_mem_new(tcg_env, offsetof(CPU[(${gen_arch_upper})]State, [(${gen_arch_upper})]_PC), "[(${gen_arch_upper})]_PC");
    //only generate if insn_count feature is enabled
    [# th:if="${insn_count}"]
    cpu_insn_count = tcg_global_mem_new(tcg_env, offsetof(CPU[(${gen_arch_upper})]State, insn_count), "[(${gen_arch_upper})]_INSN_COUNT");
    [/]

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
/*static int ex_plus_1(DisasContext *ctx, int nf)
{
    return nf + 1;
}*/

// TODO: Could potentially be removed
/*#define EX_SH(amount) \
static int ex_shift_##amount(DisasContext *ctx, int imm) \
{                                         \
    return imm << amount;                 \
}
EX_SH(1)
EX_SH(2)
EX_SH(3)
EX_SH(4)
EX_SH(12)*/

static target_ulong next_insn(DisasContext *ctx)
{
    vaddr  pc_next = ctx->base.pc_next;
    return translator_ld[(${insn_width.short})](ctx->env, &ctx->base, pc_next);
}

[# th:each="reg_file, iterState : ${register_files}"]
static TCGv get_[(${reg_file.name_lower})](DisasContext *ctx, int reg_num)
{
    assert(reg_num < [(${reg_file["size"]})]);
    [# th:each="constraint, iterState : ${reg_file.constraints}"]
    if (reg_num == [(${constraint.index})]) return ctx->const[(${reg_file.name_lower})][(${constraint.value})];
    [/]
    return cpu_[(${reg_file.name_lower})][reg_num];
}

static TCGv dest_[(${reg_file.name_lower})](DisasContext *ctx, int reg_num)
{
    assert(reg_num < [(${reg_file["size"]})]);
    [# th:each="constraint, iterState : ${reg_file.constraints}"]
    // TODO: This should be temporary instead
    if (reg_num == [(${constraint.index})]) return tcg_temp_new_i64();
    [/]
    return cpu_[(${reg_file.name_lower})][reg_num];
}

static void gen_set_[(${reg_file.name_lower})](DisasContext *ctx, int reg_num, TCGv t)
{
    if (
    true
    [# th:each="constraint, iterState : ${reg_file.constraints}"]
    && reg_num != [(${constraint.index})]
    [/]
    ) {
        // TODO: check if tl is fine. Better would be register file specific call
        tcg_gen_mov_tl(cpu_[(${reg_file.name_lower})][reg_num], t);
    }
}
[/]

/*
 * Jumps to the given target_pc and sets is_jmp to NORETURN. n indicates the jump slot
 * which is one of 0, 1 or -1. 0,1 are valid jumps slots, while -1 indicates a forced
 * move to cpu_pc with a tcg_gen_lookup_and_goto_ptr call.
 */
static void gen_goto_tb(DisasContext *ctx, int8_t n, target_ulong target_pc)
{
    if (n >= 0 && translator_use_goto_tb(&ctx->base, target_pc)) {
        tcg_gen_goto_tb(n);
        tcg_gen_movi_i64(cpu_pc, (int64_t) target_pc);
        tcg_gen_exit_tb(ctx->base.tb, n);
    } else {
        tcg_gen_movi_tl(cpu_pc, (int64_t) target_pc);
        tcg_gen_lookup_and_goto_ptr();
    }
    ctx->base.is_jmp = DISAS_NORETURN;
}

static void generate_exception(DisasContext *ctx, int excp) {
	tcg_gen_movi_i64(cpu_pc, ctx->base.pc_next);
	gen_helper_raise_exception(tcg_env, tcg_constant_i32(excp));
	ctx->base.is_jmp = DISAS_NORETURN;
}

static inline void gen_trunc(TCGv dest, TCGv arg, int bitWidth) {
    tcg_gen_andi_tl(dest, arg, (int64_t)((1ULL << bitWidth) - 1));
}

static inline void gen_exts(TCGv dest, TCGv arg, int bitWidth) {
	uint32_t leftRight = TARGET_LONG_BITS - bitWidth;
	tcg_gen_shli_tl(dest, arg, leftRight);
	tcg_gen_sari_tl(dest, dest, leftRight);
}

/*
 * Instruction translation functions.
 * Called by decode_insn() function produced by insn.deocde decode-tree.
 */

static bool decode_insn(DisasContext *ctx, uint[(${insn_width.int})]_t insn);

// Uncomment these imports to use the QEMU decode tree instead of the VADL decode tree
//#include "insn-access.h"
//#include "decode-insn.c.inc"
#include "vdt-decode.c"

//// START OF TRANSLATE FUNCTIONS ////

[# th:each="func, iterState : ${translate_functions}"]
[(${func})]
[/]


//// END OF TRANSLATE FUNCTIONS ////

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

        tcg_gen_movi_i64(cpu_pc, ctx->base.pc_next);
        gen_helper_unsupported(tcg_env);
        ctx->base.is_jmp = DISAS_NORETURN;
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
    DisasContext *ctx = container_of(db, DisasContext, base);

    // translate current insn
    translate(ctx);
    // increment program counter
    db->pc_next = db->pc_next + ([(${insn_width.int})] / [(${mem_word_size.int})]);
}

static void [(${gen_arch_lower})]_tr_tb_stop(DisasContextBase *db, CPUState *cpu)
{
    DisasContext *ctx = container_of(db, DisasContext, base);

    switch (db->is_jmp) {
    		case DISAS_TOO_MANY:
    		case DISAS_CHAIN:
    			// jump to subsequent instruction
    			gen_goto_tb(ctx, 0, db->pc_next);
    			break;
    		case DISAS_NORETURN:
    			// default behavior
    			break;
    		default:
    			g_assert_not_reached();
    	}
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
    translator_loop(cs, tb, max_insns, pc, host_pc, &[(${gen_arch_lower})]_tr_ops, &ctx.base);
}
