#include "qemu/osdep.h"
#include "user-internals.h"
#include "signal-common.h"
#include "target_signal.h"

void setup_rt_frame(int sig, struct target_sigaction *ka,
                    target_siginfo_t *info,
                    target_sigset_t *set, CPUArchState *env)
{
    fprintf(stderr, "Signals not implemented yet!\n");
}