.section .text.init
.align  4

.extern  main
.type    main, %function

.global _start
.type   _start, %function

.arm
.syntax unified

_start:
    @ The AArch32 virt spec contains firmware at 0x0 that jumps into RAM at
    @ 0x40000000, so the benchmark image only needs a plain freestanding entry.
    ldr     sp, =__stack_top
    bl      main
    b       exit

exit:
    @ Preserve the C return value so both exit mechanisms report the same code.
    mov     r2, r0

    @ Semihosting SYS_EXIT_EXTENDED expects a two-word argument block where the
    @ second word carries the application exit code.
    ldr     r1, =args
    str     r2, [r1, #4]

    @ HTIF exit is used by the generated qemu-system-a32 target. The current
    @ device model only commits the command after the high 32-bit word is
    @ written, so emit the 64-bit value as two ordered 32-bit stores.
    lsl     r0, r2, #1
    orr     r0, r0, #1
    ldr     r1, =tohost
    str     r0, [r1]
    mov     r3, #0
    str     r3, [r1, #4]

    @ Upstream qemu-system-arm exits through Arm semihosting instead of HTIF.
    ldr     r1, =args
    mov     r0, #0x20
    svc     #0x123456

1:  b       1b

.section .data
.align 3
args:
.word 0x20026
.word 0x0

@ Keep the HTIF mailboxes as standalone 8-byte objects so both the linker
@ script and the guest-side stores use the same layout.
.section .tohost, "aw", %progbits
.align 6
.global tohost
tohost: .quad 0
.size tohost, 8

.align 6
.global fromhost
fromhost: .quad 0
.size fromhost, 8
