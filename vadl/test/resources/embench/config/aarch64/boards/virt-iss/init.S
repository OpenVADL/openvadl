
.section .text.init;
.align  4;

.extern  main
.type    main, @function

.global _start
.type   _start, @function

// https://github.com/ucb-bar/libgloss-htif/blob/master/misc/crt0.S
// https://github.com/bminor/newlib/blob/master/libgloss/riscv/crt0.S
_start:
    ldr x30, =__stack_top
    mov sp, x30
    bl main
    b exit

exit:
    ldr     x1, =args        // load semihosting exit args
    str     x0, [x1, 8]      // set exit code (before x0 is overwritten)

    # exit VADL virt (HTIF)
    lsl     x0, x0, 1        // Shift exit code to 63..1
    add     x0, x0, 1        // Set bit 0 to 1 to indicate exit command
    ldr     x1, =tohost      // Load address of 'tohost' into x1
    str     x0, [x1]         // Store a0 to 'tohost'

    # exit upstream virt (semihosting)
    ldr     x1, =args        // load semihosting exit args
    mov     w0, #0x20        // SYS_EXIT operation
    hlt     #0xF000          // Trigger semihosting call

1:  b       1b               // Infinite loop

.section .data
.align 3
args:
.xword 0x20026     // ADP_Stopped_ApplicationExit command
.xword 0x0         // Exit Code

.section .tohost, "aw", @progbits
.align 6
.global tohost
tohost: .xword 0
.size tohost, 8

.align 6
.global fromhost
fromhost: .xword 0
.size fromhost, 8
