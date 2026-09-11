#include "qemu/osdep.h"
#include "qemu.h"
#include "user-internals.h"
#include "signal-common.h"

void setup_rt_frame(int sig, struct target_sigaction *ka,
                    target_siginfo_t *info,
                    target_sigset_t *set, CPU[(${gen_arch_upper})]State *env)
{
    (void)ka;
    (void)info;
    (void)set;
    (void)env;

    force_sig(sig);
}

long do_rt_sigreturn(CPU[(${gen_arch_upper})]State *env)
{
    (void)env;

    force_sig(TARGET_SIGSEGV);
    return 0;
}

void setup_sigtramp(abi_ulong sigtramp_page)
{
    (void)sigtramp_page;
}