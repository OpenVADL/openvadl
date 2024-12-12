# to communicate with the host
# check riscv-software-src/riscv-tests

.section .text.init

.global _start

_start:
        li a0, 0
        j tohost_exit # just terminate with exit code 0

# a0 exit code
tohost_exit:
        slli a0, a0, 1
        addi a0, a0, 1

        la t0, tohost
        sd a0, 0(t0)

        1: j 1b # wait for termination


.section .tohost, "aw", @progbits
.align 6; 
.global tohost; 
tohost: .dword 0; 
.size tohost, 8;    
.align 6; 
.global fromhost; 
fromhost: .dword 0; 
.size fromhost, 8;

