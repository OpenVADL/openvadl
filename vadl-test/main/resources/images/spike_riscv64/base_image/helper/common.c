extern volatile unsigned long long tohost;
extern volatile unsigned long long fromhost;

// https://github.com/riscv/riscv-test-env/blob/master/v/vm.c
void terminate(int code)
{
	while (tohost)
        fromhost = 0;
        tohost = (code << 1) | 0x1;
        while (1);
}
