#include <stdio.h>
#include <stdlib.h>

_Noreturn void __assert_fail(const char *expr, const char *file, int line, const char *func) {
    // infinite loop. when debuging it is easy to find the reason.
    while (1) {}
}

_Noreturn void __assert_func(const char *file, int line, const char *func, const char *expr) {
    // infinite loop. when debuging it is easy to find the reason.
    while (1) {}
}

_Noreturn void exit(int i) {
    // infinite loop. when debuging it is easy to find the reason.
    while (1) {}
}

_Noreturn void abort(void) {
    // infinite loop. when debuging it is easy to find the reason.
    while (1) {}
}
