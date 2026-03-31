.section .text.trap;
.align  4;

.global _halt
.type   _halt, @function
_halt:

    # halt iss
    la t0, 0xff0f
    li t1, 0xff
    sb t1, 0(t0)

    # For DTC/ISS
    li s0,93
    ebreak
