.globl main

simple:
        addi    sp,sp,-32
        sw      ra,28(sp)
        sw      s0,24(sp)
        addi    s0,sp,32
        sw      a0,-20(s0)
        sw      a1,-24(s0)
        lw      a4,-24(s0)
        lw      a5,-20(s0)
        bge a4, a5, .L3
        mv      a5,a4
.L3:
        mv      a0,a5
        lw      ra,28(sp)
        lw      s0,24(sp)
        addi    sp,sp,32
        jr      ra
main:
        addi    sp,sp,-16
        sw      ra,12(sp)
        sw      s0,8(sp)
        addi    s0,sp,16
        li      a1,1
        li      a0,0
        call    driver
        mv      a5,a0
        bne     a5,zero,.L5
        li      a1,3
        li      a0,2
        call    driver
        mv      a4,a0
        li      a5,2
        beq     a4,a5,.L6
.L5:
        li      a5,1
        j       .L8
.L6:
        li      a5,0
.L8:
        mv      a0,a5
        lw      ra,12(sp)
        lw      s0,8(sp)
        addi    sp,sp,16
        jr      ra
driver:
        addi    sp,sp,-48
        sw      ra,44(sp)
        sw      s0,40(sp)
        addi    s0,sp,48
        sw      a0,-36(s0)
        sw      a1,-40(s0)
        lui     a5,%hi(simple)
        addi    a5,a5,%lo(simple)
        sw      a5,-20(s0)
        lw      a5,-20(s0)
        lw      a1,-40(s0)
        lw      a0,-36(s0)
        jalr    x1, a5, 0
        mv      a5,a0
        mv      a0,a5
        lw      ra,44(sp)
        lw      s0,40(sp)
        addi    sp,sp,48
        jr      ra