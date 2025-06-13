.globl main

ArrayA:
        .word   1
main:
        addi    sp,sp,-16
        sw      ra,12(sp)
        sw      s0,8(sp)
        addi    s0,sp,16
        lui     a5,%hi(ArrayA)
        lw      a5,%lo(ArrayA)(a5)
        addi    a5,a5,-1
        snez    a5,a5
        andi    a5,a5,0xff
        mv      a0,a5
        lw      ra,12(sp)
        lw      s0,8(sp)
        addi    sp,sp,16
        jr      ra