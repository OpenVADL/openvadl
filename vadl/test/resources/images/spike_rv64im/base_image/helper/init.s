
.section .text.init;
.align  4;

.extern  main
.type    main, @function

.extern  _halt
.type    _halt, @function

.extern _trap_start
.type _trap_start, @function

.global _start
.type   _start, @function

_start:
    LI ra, 0
    LI gp, 0
    LI fp, 0

    LI a0, 0
    LI a1, 0
    LI a2, 0
    LI a3, 0
    LI a4, 0
    LI a5, 0
    LI a6, 0
    LI a7, 0

    LI s1, 0
    LI s2, 0
    LI s3, 0
    LI s4, 0
    LI s5, 0
    LI s6, 0
    LI s7, 0
    LI s8, 0
    LI s9, 0
    LI s10, 0
    LI s11, 0

    LI t0, 0
    LI t1, 0
    LI t2, 0
    LI t3, 0
    LI t4, 0
    LI t5, 0
    LI t6, 0

    LI sp, 0
    LI tp, 0

    LUI t0, %hi(__stack_shift)
    ADDI t0, t0, %lo(__stack_shift)
    LA tp, __stack_start
    SLL t0, s0, t0
    ADD tp, tp, t0

    LUI t0, %hi(__stack_size)
    ADDI t0, t0, %lo(__stack_size)
    ADD sp, tp, t0

    LA a5, _trap_start
    CSRW mtvec, a5

    CALL main
    CALL terminate
