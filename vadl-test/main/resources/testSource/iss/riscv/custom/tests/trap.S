# to communicate with the host
# check riscv-software-src/riscv-tests

.section .text.init
.global _start

_start:
        la t0, _trap_handler
        csrrw t0, mtvec, t0
        j main 


tohost_exit:
        slli a0, a0, 1
        addi a0, a0, 1

        la t0, tohost
        sd a0, 0(t0)

        1: j 1b

_trap_handler:
        j tohost_exit

.section .text
main:
		li a0, 0 # exit with code 0
    ecall


.section .tohost, "aw", @progbits
.align 6; 
.global tohost; 
tohost: .dword 0; 
.size tohost, 8;    
.align 6; 
.global fromhost; 
fromhost: .dword 0; 
.size fromhost, 8;
