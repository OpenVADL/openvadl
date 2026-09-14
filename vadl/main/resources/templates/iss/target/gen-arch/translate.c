#include "qemu/osdep.h"
#include "tcg/tcg.h"
#include "exec/exec-all.h"
#include "exec/log.h"
#include "exec/translator.h"
#include "qemu/qemu-print.h"
#include "tcg/tcg-op.h"
#include "tcg/tcg-op-gvec.h"
#include "cpu-bits.h"
#include "trace.h"
#include "vadl-builtins.h"

#include "exec/helper-proto.h"
#include "exec/helper-gen.h"
#define HELPER_H "helper.h"
#include "exec/helper-info.c.inc"
#undef  HELPER_H

// define the registers tcgs
[# th:each="reg : ${register_tensors}" th:if="${reg.is_tcg}"]
static TCGv cpu_[(${reg.name_lower})][(${reg.c_array_def})];
[/]

/* We have a single condition exit.
   So reaching the end of the branch instruction means, we want to execute the
   following instruction as well -> we want to chain the default (no taking) branch with
   the next instruction. This is handled in the tb_stop function by calling goto_tb with
   the next instruction PC.
 */
#define DISAS_CHAIN  DISAS_TARGET_0
#define DISAS_EXIT   DISAS_TARGET_1

typedef struct DisasContext {
  DisasContextBase base;

  CPU[(${gen_arch_upper})]State *env;

  target_ulong pc_curr;

  // constraint value constants
  [# th:each="reg, iterState : ${register_tensors}"][# th:each="constraint, iterState : ${reg.constraints}"]
  TCGv [(${constraint.tcg_name})];
  [/][/]

  struct {
    [# th:each="reg, iterState : ${register_tensors}"][# th:if="${reg.is_tb_state}"]
    uint[(${reg.cpu_state_type_width})]_t [(${reg.name_lower})];
    [/][/]
  } tb_state;

  [# th:if="${mem_bi_endian}"]bool endian_cond;[/]

} DisasContext;

static bool swap_insn(const DisasContext *ctx) {
  // default byte order is LE, but VDT assumes BE
[# th:if="${mem_bi_endian}"]
#if TARGET_BIG_ENDIAN
  return [# th:unless="${mem_big_endian}"]![/]ctx->endian_cond;
#else
  return [# th:if="${mem_big_endian}"]![/]ctx->endian_cond;
#endif
[/][# th:unless="${mem_bi_endian}"]
  return true;
[/]}

void [(${gen_arch_lower})]_tcg_init(void)
{
[(${tcg_v_init_code})]
}

/*
 * Helper functions called during translation:
 *
 *    - get_<reg_file>()     ... returns a TCGv (variable) for the given register
 *    - dest_<reg_file>()    ... returns a TCGv (variable) to store result in
 *    - gen_set_<reg_file>() ... generates a write of the given TCGv to the given register
 *    - gen_goto_tb()        ... generates a jump with a given diff
 *
 */

static target_ulong next_insn(DisasContext *ctx)
{
    vaddr  pc_next = ctx->base.pc_next;
    return translator_ld[(${insn_width.short})]_swap(ctx->env, &ctx->base, pc_next, swap_insn(ctx));
}

[# th:each="reg : ${register_tensors}" th:if="${reg.is_gvec_capable}"]
static inline uint32_t [(${reg.gvec_offset_helper_name})](DisasContext *ctx[(${reg.gvec_offset_params})])
{   [# th:each="dim : ${reg.gvec_offset_dims}"]
    assert( [(${dim.arg_name})] < [(${dim["size"]})]); [/]
    (void) ctx;
    return [(${reg.gvec_offset_expr})];
}
[/]

[# th:each="reg : ${register_tensors}" th:if="${reg.is_tcg}"]
static TCGv get_[(${reg.name_lower})](DisasContext *ctx [(${reg.getter_params})])
{   [# th:each="dim : ${reg.index_dims}"]
    assert( [(${dim.arg_name})] < [(${dim["size"]})]); [/]
    [# th:each="constraint : ${reg.constraints}"]
    if ([(${constraint.check})]) return ctx->[(${constraint.tcg_name})];
    [/]
    return cpu_[(${reg.name_lower})][# th:each="dim : ${reg.index_dims}"][ [(${dim.arg_name})]][/];
}

static TCGv dest_[(${reg.name_lower})](DisasContext *ctx [(${reg.getter_params})])
{   [# th:each="dim : ${reg.index_dims}"]
    assert( [(${dim.arg_name})] < [(${dim["size"]})]); [/]
    [# th:each="constraint : ${reg.constraints}"]
    if ([(${constraint.check})]) return tcg_temp_new();
    [/]
    return cpu_[(${reg.name_lower})][# th:each="dim : ${reg.index_dims}"][ [(${dim.arg_name})]][/];
}
[/]

[# th:each="alias : ${alias_accessors}"]
static TCGv get_[(${alias.name_lower})](DisasContext *ctx [(${alias.getter_params})])
{   [# th:each="dim : ${alias.index_dims}"]
    assert( [(${dim.arg_name})] < [(${dim["size"]})]); [/]
    [# th:if="${alias.has_zero_check}"]
    if ([(${alias.zero_check})]) return tcg_constant_i[(${alias.value_width})](0);
    [/]
    [# th:if="${alias.has_slice}"]
    TCGv base = get_[(${alias.base_name_lower})](ctx[(${alias.forward_args})]);
    TCGv result = tcg_temp_new_i[(${alias.value_width})]();
    tcg_gen_extract_i[(${alias.value_width})](result, base, [(${alias.slice_lsb})], [(${alias.slice_width})]);
    return result;
    [/] [# th:unless="${alias.has_slice}"]
    return get_[(${alias.base_name_lower})](ctx[(${alias.forward_args})]);
    [/]
}

static TCGv dest_[(${alias.name_lower})](DisasContext *ctx [(${alias.getter_params})])
{   [# th:each="dim : ${alias.index_dims}"]
    assert( [(${dim.arg_name})] < [(${dim["size"]})]); [/]
    [# th:if="${alias.has_zero_check}"]
    if ([(${alias.zero_check})]) return tcg_temp_new_i[(${alias.value_width})]();
    [/]
    return dest_[(${alias.base_name_lower})](ctx[(${alias.forward_args})]);
}
[/]

static void gen_update_pc(DisasContext *ctx, target_ulong pc) {
    tcg_gen_movi_tl(cpu_[(${pc_info.accessor})], pc);
}

static void gen_update_pc_diff(DisasContext *ctx, target_long diff) {
    target_ulong dest = ctx->base.pc_next + diff;
    gen_update_pc(ctx, dest);
}

static void gen_raise_ume_syscall(DisasContext *ctx) {
    gen_update_pc(ctx, ctx->pc_curr);

    gen_helper_raise_ume_syscall(tcg_env);

    ctx->base.is_jmp = DISAS_NORETURN;
}


/*
 * Jumps to the given target_pc and sets is_jmp to NORETURN. n indicates the jump slot
 * which is one of 0, 1 or -1. 0,1 are valid jumps slots, while -1 indicates a forced
 * move to cpu_pc with a tcg_gen_lookup_and_goto_ptr call.
 */
static void gen_goto_tb(DisasContext *ctx, int8_t n, target_ulong target_pc)
{
    if (n >= 0 && translator_use_goto_tb(&ctx->base, target_pc)) {
        tcg_gen_goto_tb(n);
        gen_update_pc(ctx, target_pc);
        tcg_gen_exit_tb(ctx->base.tb, n);
    } else {
        gen_update_pc(ctx, target_pc);
        tcg_gen_lookup_and_goto_ptr();
    }
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
 *  Helpers for TB chaining/exiting
 *
 *    - is_jmp_create_state()
 *    - is_jmp_direct_jmp()
 *    - is_jmp_indirect_jmp()
 *    - is_jmp_static_tb_state_write()
 *    - is_jmp_dynamic_tb_state_write()
 *    - is_jmp_set()
 *
 */

typedef struct IsJmpState {
	bool end_tb;
	bool can_chain;
	bool jmpslt_free;
} IsJmpState;

IsJmpState is_jmp_create_state() {
  IsJmpState state = {
    .end_tb = false,
    .can_chain = true,
    .jmpslt_free = true
  };
  return state;
}

void is_jmp_direct_jmp(DisasContext *ctx, IsJmpState *s, bool use_jmpslt, target_ulong addr) {
	if (use_jmpslt && s->can_chain && s->jmpslt_free) {
		s->jmpslt_free = false;
		gen_goto_tb(ctx, 1, addr);
	} else {
		// generates tcg_gen_lookup_and_goto_ptr()
		gen_goto_tb(ctx, -1, addr);
	}
	s->end_tb = true;
}

void is_jmp_indirect_jmp(IsJmpState *s) {
  // pc update happens in translation function
	tcg_gen_lookup_and_goto_ptr();
	s->end_tb = true;
}

void is_jmp_static_tb_state_write(IsJmpState *s) {
	s->end_tb = true;
}

void is_jmp_dynamic_tb_state_write(IsJmpState *s) {
	s->end_tb = true;
	s->can_chain = false;
}

void is_jmp_helper_call(IsJmpState *s) {
  s->end_tb = true;
}

void is_jmp_set(DisasContext *ctx, IsJmpState* s) {
	if (s->end_tb) {
		if (s->can_chain) {
			// the TB must end, but we can chain
			ctx->base.is_jmp = DISAS_CHAIN;
		} else {
			// the TB must end and there may be no jump instr
			// -> need to generate TCG exit TB instr
			ctx->base.is_jmp = DISAS_EXIT;
		}
	}
}

/*
 * Instruction translation functions.
 * Called by decode_insn() function produced by insn.deocde decode-tree.
 */

static uint8_t decode_insn(DisasContext *ctx, uint[(${insn_width.int})]_t insn);

// Include the generated VADL decode tree
#include "vdt-decode.c"

// Include translation functions
[# th:each="func, iterState : ${trans_includes}"]
#include "[(${func})]"
[/]


/*
 *  Core translation mechanism functions:
 *
 *    - translate()
 *    - gen_intermediate_code()
 *
 */
static void translate(DisasContext *ctx)
{
    // TODO: In the future, let the decoder handle fetching & advancing the PC
    uint32_t insn = next_insn(ctx);
    uint8_t len = decode_insn(ctx, insn);

    if (len) {
        // Increment program counter
        ctx->base.pc_next += len;
        return;
    }

    error_report("[[(${gen_arch_upper})]] translate, illegal instr, pc: 0x%04 , insn: 0x%04x\n", ctx->base.pc_next, insn);

    gen_update_pc_diff(ctx, 0);
    gen_helper_unsupported(tcg_env);
    ctx->base.is_jmp = DISAS_NORETURN;

    // Increase pc_next so the TB size is not empty (Validated by QEMU translate-all.c:279)
    ctx->base.pc_next += 1;
}

static void [(${gen_arch_lower})]_tr_init_disas_context(DisasContextBase *db, CPUState *cs)
{
    DisasContext *ctx = container_of(db, DisasContext, base);
    CPU[(${gen_arch_upper})]State *env = cpu_env(cs);
    [(${gen_arch_upper})]CPUClass *mcc = [(${gen_arch_upper})]_CPU_GET_CLASS(cs);
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);

    ctx->env = env;
    [# th:each="reg, iterState : ${register_tensors}"][# th:each="constraint, iterState : ${reg.constraints}"]
    ctx->[(${constraint.tcg_name})] = tcg_constant_i[(${reg.value_width})]([(${constraint.value})]);
    [/][/]

    uint32_t flags = ctx->base.tb->flags;
    uint64_t off = 0;
    [# th:each="reg, iterState : ${register_tensors}"][# th:if="${reg.is_tb_state}"]
    ctx->tb_state.[(${reg.name_lower})] = 0;
    [# th:each="part : ${reg.tb_state_parts}"]
    ctx->tb_state.[(${reg.name_lower})] |= ((flags >> off) & [(${part.mask})]) << [(${part.lsb})];
    off += [(${part.width})];[/][/][/]

    [# th:if="${mem_bi_endian}"]ctx->endian_cond = [(${bi_endian_condition})];[/]
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
    target_ulong pc = db->pc_next;

    ctx->pc_curr = pc;
    // translate current insn (and increment program counter)
    translate(ctx);
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
        case DISAS_EXIT:
            // force tb exit
            gen_update_pc(ctx, db->pc_next);
            tcg_gen_exit_tb(NULL, 0);
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
