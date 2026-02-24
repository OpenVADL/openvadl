#include "qemu/osdep.h"
#include "cpu.h"
#include "exec/exec-all.h"
#include "tcg/tcg-op.h"
#include "exec/translator.h"
#include "exec/helper-proto.h"
#include "exec/helper-gen.h"
#include "exec/log.h"

#include "decode-insn32.c.inc"

static TCGv cpu_gpr[32];
static TCGv cpu_pc;

typedef struct DisasContext {
    DisasContextBase base;
    CPUArchState *env;
} DisasContext;

static void mytarget_tr_init_disas_context(DisasContextBase *dcbase, CPUState *cs)
{
    DisasContext *ctx = container_of(dcbase, DisasContext, base);
    ctx->env = cpu_env(cs);
}

static void mytarget_tr_tb_start(DisasContextBase *db, CPUState *cpu)
{
}

static bool trans_add(DisasContext *ctx, arg_add *a)
{
    tcg_gen_add_tl(cpu_gpr[a->rd], cpu_gpr[a->rs1], cpu_gpr[a->rs2]);
    return true;
}

static void mytarget_tr_translate_insn(DisasContextBase *dcbase, CPUState *cpu)
{
    DisasContext *ctx = container_of(dcbase, DisasContext, base);
    uint32_t opcode;

    opcode = translator_ldl(ctx->env, ctx->base.pc_next);

    if (!decode_insn32(ctx, opcode)) {
        gen_helper_raise_exception(tcg_env, tcg_constant_i32(EXCP_ILLEGAL));
    }

    ctx->base.pc_next += 4;
    
    if (ctx->base.is_jmp == DISAS_NEXT) {
        target_ulong page_start = ctx->base.pc_first & TARGET_PAGE_MASK;
        if (ctx->base.pc_next - page_start >= TARGET_PAGE_SIZE) {
            ctx->base.is_jmp = DISAS_TOO_MANY;
        }
    }
}

static void mytarget_tr_tb_stop(DisasContextBase *dcbase, CPUState *cpu)
{
    DisasContext *ctx = container_of(dcbase, DisasContext, base);

    switch (ctx->base.is_jmp) {
    case DISAS_TOO_MANY:
        tcg_gen_st_tl(tcg_constant_tl(ctx->base.pc_next), tcg_env, offsetof(CPUArchState, pc));
        tcg_gen_exit_tb(NULL, 0);
        break;
    case DISAS_NORETURN:
        break;
    default:
        g_assert_not_reached();
    }
}

static bool mytarget_tr_disas_log(const DisasContextBase *dcbase, CPUState *cpu, 
                                  FILE *logfile)
{
    fprintf(logfile, "IN: (Disassembly not implemented)\n");
    return true; 
}

static const TranslatorOps mytarget_tr_ops = {
    .init_disas_context = mytarget_tr_init_disas_context,
    .tb_start           = mytarget_tr_tb_start,
    .insn_start         = NULL,
    .translate_insn     = mytarget_tr_translate_insn,
    .tb_stop            = mytarget_tr_tb_stop,
    .disas_log          = mytarget_tr_disas_log,
};

void gen_intermediate_code(CPUState *cs, TranslationBlock *tb, int *max_insns,
                           target_ulong pc, void *host_pc)
{
    DisasContext ctx;
    translator_loop(cs, tb, max_insns, pc, host_pc, 
                    &mytarget_tr_ops, &ctx.base);
}