
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

// https://github.com/ucb-bar/libgloss-htif/blob/master/misc/crt0.S
// https://github.com/bminor/newlib/blob/master/libgloss/riscv/crt0.S
_start:
    li ra, 0
    li gp, 0
    li fp, 0

    li a0, 0
    li a1, 0
    li a2, 0
    li a3, 0
    li a4, 0
    li a5, 0
    li a6, 0
    li a7, 0

    li s1, 0
    li s2, 0
    li s3, 0
    li s4, 0
    li s5, 0
    li s6, 0
    li s7, 0
    li s8, 0
    li s9, 0
    li s10, 0
    li s11, 0

    li t0, 0
    li t1, 0
    li t2, 0
    li t3, 0
    li t4, 0
    li t5, 0
    li t6, 0

    li sp, 0
    li tp, 0

    lui t0, %hi(__stack_shift)
    addi t0, t0, %lo(__stack_shift)
    la tp, __stack_start
    sll t0, s0, t0
    add tp, tp, t0

    lui t0, %hi(__stack_size)
    addi t0, t0, %lo(__stack_size)
    add sp, tp, t0

    // Set trap vector
    la a5, _trap_start
    csrw mtvec, a5

    call main
    // li  a0, 1
    call terminate
