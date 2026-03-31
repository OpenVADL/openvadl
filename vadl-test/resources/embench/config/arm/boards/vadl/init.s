.section .sdata,"aw"
.global begin_signature
.size   begin_signature,4

begin_signature:
        .word   0xFFFFFFFF

.global end_signature
.size   end_signature,4

end_signature:
        .word 0


.extern main
.type   main,"function"

.extern _stop
.type   _stop,"function"

.global _start
.type   _start,"function"

.extern __stack_top
.type   __stack_top,"object"

.section .text.init,"ax"
.align  4

_start:
        ldr x30, =__stack_top
        msr NZCV, xzr // just to align the execution traces with qemu
        mov sp, x30
        bl main
        b _stop

