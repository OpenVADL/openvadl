.section .text.trap;
.align  4;

.global _halt
.type   _halt, @function
_halt:
    # For CAS/HDL
    lui x2, 0xfffff
    csrrw x1, 0xfff, x2

    # For DTC/ISS
    li s0,93
    ebreak
