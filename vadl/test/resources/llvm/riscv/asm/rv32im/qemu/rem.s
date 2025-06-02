.globl main

a:
        .word   7
main:
        addi    sp,sp,-16
        sw      ra,12(sp)
        sw      s0,8(sp)
        addi    s0,sp,16
        lui     a5,%hi(a)
        lw      a4,%lo(a)(a5)
        srai    a5,a4,31
        srli    a5,a5,31
        add     a4,a4,a5
        andi    a4,a4,1
        sub     a5,a4,a5
        addi    a5,a5,-1
        snez    a5,a5
        andi    a5,a5,0xff
        mv      a0,a5
        lw      ra,12(sp)
        lw      s0,8(sp)
        addi    sp,sp,16
        jr      ra