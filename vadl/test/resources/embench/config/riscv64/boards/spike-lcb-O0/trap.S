.section .text.trap;
.align 4;

.global _trap_start
.type _trap_start, @function

_trap_start:
    j _trap_mcause # 0x00
    j _trap_mcause # 0x04
    j _trap_mcause # 0x08
    j _trap_mcause # 0x0c # ebreak
    j _trap_mcause # 0x10
    j _trap_mcause # 0x14
    j _trap_mcause # 0x18
    j _trap_mcause # 0x1c
    j _trap_mcause # 0x20
    j _trap_mcause # 0x24
    j _trap_mcause # 0x28
    j _trap_mcause # 0x2c
    j _trap_mcause # 0x30

_trap_mcause:
    lui a0, 1
    call terminate
