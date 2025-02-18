#include <stdint.h>
#include <stddef.h>

extern volatile uint64_t tohost;
extern volatile uint64_t fromhost;

// https://github.com/riscv/riscv-test-env/blob/master/v/vm.c
void terminate(int code)
{
    while (tohost)
        fromhost = 0;
    tohost = (code << 1) | 0x1;
    while (1);
}

