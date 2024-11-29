.section .text.trap;
.align 4;

.global _trap_start
.type _trap_start, @function

_trap_start:
    J _trap_mcause # 0x00
    J _trap_mcause # 0x04
    J _trap_mcause # 0x08
    J _trap_mcause # 0x0c # ebreak
    J _trap_mcause # 0x10
    J _trap_mcause # 0x14
    J _trap_mcause # 0x18
    J _trap_mcause # 0x1c
    J _trap_mcause # 0x20
    J _trap_mcause # 0x24
    J _trap_mcause # 0x28
    J _trap_mcause # 0x2c
    J _trap_mcause # 0x30

_trap_mcause:
    LUI a0, 1
    CALL terminate
