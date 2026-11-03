/*
 * setjmp/longjmp for the LCB rv32im toolchain, written in the syntax understood by the generated
 * assembler (no assembler macros, no symbolic branch targets).
 *
 * jmp_buf layout: ra, s0-s11, sp (14 words). This fits into the RISC-V jmp_buf declared by
 * picolibc's <machine/setjmp.h>, which the LCB clang selects via the predefined __riscv macro.
 */

/* int setjmp(jmp_buf env); */
        .section .text.setjmp,"ax",@progbits
        .globl  setjmp
        .type   setjmp,@function
setjmp:
        sw      ra,0(a0)
        sw      s0,4(a0)
        sw      s1,8(a0)
        sw      s2,12(a0)
        sw      s3,16(a0)
        sw      s4,20(a0)
        sw      s5,24(a0)
        sw      s6,28(a0)
        sw      s7,32(a0)
        sw      s8,36(a0)
        sw      s9,40(a0)
        sw      s10,44(a0)
        sw      s11,48(a0)
        sw      sp,52(a0)
        addi    a0,zero,0
        jr      ra
        .size   setjmp,.-setjmp

/* void longjmp(jmp_buf env, int val); */
        .section .text.longjmp,"ax",@progbits
        .globl  longjmp
        .type   longjmp,@function
longjmp:
        lw      ra,0(a0)
        lw      s0,4(a0)
        lw      s1,8(a0)
        lw      s2,12(a0)
        lw      s3,16(a0)
        lw      s4,20(a0)
        lw      s5,24(a0)
        lw      s6,28(a0)
        lw      s7,32(a0)
        lw      s8,36(a0)
        lw      s9,40(a0)
        lw      s10,44(a0)
        lw      s11,48(a0)
        lw      sp,52(a0)
        /* return (val == 0) ? 1 : val */
        sltiu   a0,a1,1
        add     a0,a0,a1
        jr      ra
        .size   longjmp,.-longjmp

