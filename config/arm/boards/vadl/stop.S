.text
.global _stop
.type   _stop,"function"

_stop:
     #ifdef STOP_VADL_BRK
        // For VADL when using `stop with @BRK = PC = 0`
        ldr x1, =begin_signature
        str w0, [x1]
        brk 0
     #endif

     #ifdef STOP_LINUX_UME
        // For e.g. gem5/qemu UME
        // __syscall(SYS_exit, result_of_main);
        mov x8, 93
        svc 0
     #endif

     #ifdef STOP_SEMIHOISTING
        // ARM semihosting, as supported by `qemu-system-aarch64 -semihosting` and qemu UME
        // https://github.com/ARM-software/abi-aa/blob/main/semihosting/semihosting.rst

        // Perform `semi_exit(result_of_main);`
        // (see https://git.linaro.org/people/peter.maydell/semihosting-tests.git/tree/)
        sub     sp, sp, #32
        stp     x29, x30, [sp, #16] 
        add     x29, sp, #16
        mov     w8, #38  
        sxtw    x9, w0
        mov     x1, sp
        movk    w8, #2, lsl #16
        mov     w0, #24       
        stp     x8, x9, [sp]
                //  __semi_call
                mov x8, 0x20
                hlt 0xf000
     #endif

        b . // Loop, unreachable