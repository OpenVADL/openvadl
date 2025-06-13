.globl main

option1:
        addi    sp,sp,-16
        sw      ra,12(sp)
        sw      s0,8(sp)
        addi    s0,sp,16
        li      a5,0
        mv      a0,a5
        lw      ra,12(sp)
        lw      s0,8(sp)
        addi    sp,sp,16
        jr      ra
option2:
        addi    sp,sp,-16
        sw      ra,12(sp)
        sw      s0,8(sp)
        addi    s0,sp,16
        li      a5,1
        mv      a0,a5
        lw      ra,12(sp)
        lw      s0,8(sp)
        addi    sp,sp,16
        jr      ra
option3:
        addi    sp,sp,-16
        sw      ra,12(sp)
        sw      s0,8(sp)
        addi    s0,sp,16
        li      a5,2
        mv      a0,a5
        lw      ra,12(sp)
        lw      s0,8(sp)
        addi    sp,sp,16
        jr      ra
main:
        addi    sp,sp,-32
        sw      ra,28(sp)
        sw      s0,24(sp)
        addi    s0,sp,32
        lui     a5,%hi(option1)
        addi    a5,a5,%lo(option1)
        sw      a5,-28(s0)
        lui     a5,%hi(option2)
        addi    a5,a5,%lo(option2)
        sw      a5,-24(s0)
        lui     a5,%hi(option3)
        addi    a5,a5,%lo(option3)
        sw      a5,-20(s0)
        lw      a5,-28(s0)
        jalr    x1, a5, 0
        mv      a5,a0
        bne     a5,zero,.L8
        lw      a5,-24(s0)
        jalr    x1, a5, 0
        mv      a4,a0
        li      a5,1
        bne     a4,a5,.L8
        lw      a5,-20(s0)
        jalr    x1, a5, 0
        mv      a4,a0
        li      a5,2
        beq     a4,a5,.L9
.L8:
        li      a5,1
        j       .L11
.L9:
        li      a5,0
.L11:
        mv      a0,a5
        lw      ra,28(sp)
        lw      s0,24(sp)
        addi    sp,sp,32
        jr      ra