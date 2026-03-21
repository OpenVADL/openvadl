/*
 * Emulation of Linux signals for [(${gen_arch_lower})] user-mode.
 */

#include "qemu/osdep.h"
#include "qemu.h"
#include "user-internals.h"
#include "signal-common.h"
#include "linux-user/trace.h"

/*
 * Minimal sigcontext matching [(${gen_arch_lower})] state:
 * - pc
 * - x1..x31 (x0 is always zero and omitted)
 */
struct target_sigcontext {
    abi_long [(${pc_reg.name_lower})];
    abi_long gpr[31];
};

struct target_ucontext {
    abi_ulong uc_flags;
    abi_ptr uc_link;
    target_stack_t uc_stack;
    target_sigset_t uc_sigmask;
    uint8_t __unused[1024 / 8 - sizeof(target_sigset_t)];
    struct target_sigcontext uc_mcontext QEMU_ALIGNED(16);
};

struct target_rt_sigframe {
    struct target_siginfo info;
    struct target_ucontext uc;
};

static abi_ulong get_sigframe(struct target_sigaction *ka,
                              CPU[(${gen_arch_upper})]State *regs, size_t framesize)
{
    abi_ulong sp = get_sp_from_cpustate(regs);

    if (on_sig_stack(sp) && !likely(on_sig_stack(sp - framesize))) {
        return -1L;
    }

    sp = target_sigsp(sp, ka) - framesize;
    sp &= ~0xf;

    return sp;
}

static void setup_sigcontext(struct target_sigcontext *sc, CPU[(${gen_arch_upper})]State *env)
{
    int i;

    __put_user(env->[(${pc_reg.name_lower})], &sc->[(${pc_reg.name_lower})]);
    for (i = 1; i < 32; i++) {
        __put_user(env->x[i], &sc->gpr[i - 1]);
    }
}

static void setup_ucontext(struct target_ucontext *uc,
                           CPU[(${gen_arch_upper})]State *env, target_sigset_t *set)
{
    int i;

    __put_user(0, &(uc->uc_flags));
    __put_user(0, &(uc->uc_link));

    target_save_altstack(&uc->uc_stack, env);

    for (i = 0; i < TARGET_NSIG_WORDS; i++) {
        __put_user(set->sig[i], &(uc->uc_sigmask.sig[i]));
    }

    setup_sigcontext(&uc->uc_mcontext, env);
}

void setup_rt_frame(int sig, struct target_sigaction *ka,
                    target_siginfo_t *info,
                    target_sigset_t *set, CPU[(${gen_arch_upper})]State *env)
{
    abi_ulong frame_addr;
    struct target_rt_sigframe *frame;

    frame_addr = get_sigframe(ka, env, sizeof(*frame));
    trace_user_setup_rt_frame(env, frame_addr);

    if (!lock_user_struct(VERIFY_WRITE, frame, frame_addr, 0)) {
        goto badframe;
    }

    setup_ucontext(&frame->uc, env, set);
    frame->info = *info;

    env->[(${pc_reg.name_lower})] = ka->_sa_handler;
    env->x[[(${gen_arch_upper})]_REG_SP] = frame_addr;
    env->x[[(${gen_arch_upper})]_REG_A0] = sig;
    env->x[[(${gen_arch_upper})]_REG_A1] = frame_addr + offsetof(struct target_rt_sigframe, info);
    env->x[[(${gen_arch_upper})]_REG_A2] = frame_addr + offsetof(struct target_rt_sigframe, uc);
    env->x[[(${gen_arch_upper})]_REG_RA] = default_rt_sigreturn;

    return;

badframe:
    unlock_user_struct(frame, frame_addr, 1);
    if (sig == TARGET_SIGSEGV) {
        ka->_sa_handler = TARGET_SIG_DFL;
    }
    force_sig(TARGET_SIGSEGV);
}

static void restore_sigcontext(CPU[(${gen_arch_upper})]State *env, struct target_sigcontext *sc)
{
    int i;

    __get_user(env->[(${pc_reg.name_lower})], &sc->[(${pc_reg.name_lower})]);
    for (i = 1; i < 32; ++i) {
        __get_user(env->x[i], &sc->gpr[i - 1]);
    }
}

static void restore_ucontext(CPU[(${gen_arch_upper})]State *env, struct target_ucontext *uc)
{
    sigset_t blocked;
    target_sigset_t target_set;
    int i;

    target_sigemptyset(&target_set);
    for (i = 0; i < TARGET_NSIG_WORDS; i++) {
        __get_user(target_set.sig[i], &(uc->uc_sigmask.sig[i]));
    }

    target_to_host_sigset_internal(&blocked, &target_set);
    set_sigmask(&blocked);

    restore_sigcontext(env, &uc->uc_mcontext);
}

long do_rt_sigreturn(CPU[(${gen_arch_upper})]State *env)
{
    struct target_rt_sigframe *frame;
    abi_ulong frame_addr;

    frame_addr = env->x[[(${gen_arch_upper})]_REG_SP];
    trace_user_do_sigreturn(env, frame_addr);
    if (!lock_user_struct(VERIFY_READ, frame, frame_addr, 1)) {
        goto badframe;
    }

    restore_ucontext(env, &frame->uc);
    target_restore_altstack(&frame->uc.uc_stack, env);

    unlock_user_struct(frame, frame_addr, 0);
    return -QEMU_ESIGRETURN;

badframe:
    unlock_user_struct(frame, frame_addr, 0);
    force_sig(TARGET_SIGSEGV);
    return 0;
}

void setup_sigtramp(abi_ulong sigtramp_page)
{
    uint32_t *tramp = lock_user(VERIFY_WRITE, sigtramp_page, 8, 0);
    assert(tramp != NULL);

    __put_user(0x08b00893, tramp + 0);  /* li a7, 139 = __NR_rt_sigreturn */
    __put_user(0x00000073, tramp + 1);  /* ecall */

    default_rt_sigreturn = sigtramp_page;
    unlock_user(tramp, sigtramp_page, 8);
}
