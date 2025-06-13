.globl main

main:
        addi    sp,sp,-32
        sw      ra,28(sp)
        sw      s0,24(sp)
        addi    s0,sp,32
        li      a5,1
        sw      a5,-20(s0)
        lw      a5,-20(s0)
        addi    a5,a5,-1
        snez    a5,a5
        andi    a5,a5,0xff
        mv      a0,a5
        lw      ra,28(sp)
        lw      s0,24(sp)
        addi    sp,sp,32
        jr      ra