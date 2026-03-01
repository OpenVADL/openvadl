# to communicate with the host
# check riscv-software-src/riscv-tests

.section .text.init
.global _start
_start:
    mov     x0, #((0b11<<20) | (0b11 << 16))
    msr     cpacr_el1, x0        // enable floating point and SVE
    mov     x2,  #0              // test case counter
    addvl   x3,  x2,  #1         // vl.b
    lsr     x4,  x3,  #1         // vl.h
    lsr     x5,  x3,  #2         // vl.s
    lsr     x6,  x3,  #3         // vl.d
    ptrue   p8.b
    ptrue   p0.b,pow2
    ptrue   p1.h,pow2
    ptrue   p2.s,pow2
    ptrue   p3.d,pow2
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x6
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x04
    ptrue   p0.b,vl1
    ptrue   p1.h,vl1
    ptrue   p2.s,vl1
    ptrue   p3.d,vl1
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, #1
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, #1
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, #1
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, #1
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x08
    ptrue   p0.b,vl2
    ptrue   p1.h,vl2
    ptrue   p2.s,vl2
    orr     p3.b,p8/z,p0.b,p1.b
    orr     p3.b,p8/z,p3.b,p2.b
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, #2
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, #2
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, #2
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, #4
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x0c
    ptrue   p0.b,vl3
    ptrue   p1.h,vl3
    ptrue   p2.s,vl2
    orr     p3.b,p8/z,p0.b,p1.b
    orr     p3.b,p8/z,p3.b,p2.b
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, #3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, #3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, #2
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, #4
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x10
    ptrue   p0.b,vl4
    ptrue   p1.b,vl5
    ptrue   p2.b,vl6
    ptrue   p3.b,vl7
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, #4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, #5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, #6
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, #7
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x14
    ptrue   p0.b,mul3
    ptrue   p1.h,mul3
    ptrue   p2.s,mul3
    ptrue   p3.d,mul3
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    mov     x8,  #3
    udiv    x7,  x3, x8
    mul     x7,  x7, x8
    cmp     x13, x7
    b.ne    exit
    add     x2,  x2, #1
    udiv    x7,  x4, x8
    mul     x7,  x7, x8
    cmp     x14, x7
    b.ne    exit
    add     x2,  x2, #1
    udiv    x7,  x5, x8
    mul     x7,  x7, x8
    cmp     x15, x7
    b.ne    exit
    add     x2,  x2, #1
    udiv    x7,  x6, x8
    mul     x7,  x7, x8
    cmp     x16, x7
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x18
    ptrue   p0.b,mul4
    ptrue   p1.h,mul4
    ptrue   p2.s,mul4
    ptrue   p3.d,mul4
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    mov     x8,  #4
    udiv    x7,  x3, x8
    mul     x7,  x7, x8
    cmp     x13, x7
    b.ne    exit
    add     x2,  x2, #1
    udiv    x7,  x4, x8
    mul     x7,  x7, x8
    cmp     x14, x7
    b.ne    exit
    add     x2,  x2, #1
    udiv    x7,  x5, x8
    mul     x7,  x7, x8
    cmp     x15, x7
    b.ne    exit
    add     x2,  x2, #1
    udiv    x7,  x6, x8
    mul     x7,  x7, x8
    cmp     x16, x7
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x1c
    ptrue   p0.b
    ptrue   p1.h
    ptrue   p2.s
    ptrue   p3.d
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x6
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x20
    ptrue   p0.b,vl8
    ptrue   p1.b,vl16
    ptrue   p2.b,vl32
    ptrue   p3.b,vl64
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    mov     x17, #8
    mov     x18, #16
    mov     x19, #32
    mov     x20, #64
    cmp     x3,  x17
    csel    x17, x17, xzr, ge
    cmp     x3,  x18
    csel    x18, x18, xzr, ge
    cmp     x3,  x19
    csel    x19, x19, xzr, ge
    cmp     x3,  x20
    csel    x20, x20, xzr, ge
    cmp     x13, x17
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x18
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x19
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x20
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x24
    ptrue   p0.h,vl8
    ptrue   p1.h,vl16
    ptrue   p2.h,vl32
    ptrue   p3.h,vl64
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    mov     x17, #8
    mov     x18, #16
    mov     x19, #32
    mov     x20, #64
    cmp     x4,  x17
    csel    x17, x17, xzr, ge
    cmp     x4,  x18
    csel    x18, x18, xzr, ge
    cmp     x4,  x19
    csel    x19, x19, xzr, ge
    cmp     x4,  x20
    csel    x20, x20, xzr, ge
    cmp     x13, x17
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x18
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x19
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x20
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x28
    ptrue   p0.s,vl8
    ptrue   p1.s,vl16
    ptrue   p2.s,vl32
    ptrue   p3.s,vl64
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    mov     x17, #8
    mov     x18, #16
    mov     x19, #32
    mov     x20, #64
    cmp     x5,  x17
    csel    x17, x17, xzr, ge
    cmp     x5,  x18
    csel    x18, x18, xzr, ge
    cmp     x5,  x19
    csel    x19, x19, xzr, ge
    cmp     x5,  x20
    csel    x20, x20, xzr, ge
    cmp     x13, x17
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x18
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x19
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x20
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x1c
    ptrue   p0.d,vl8
    ptrue   p1.d,vl16
    ptrue   p2.d,vl32
    ptrue   p3.d,vl64
    cntp    x13, p0, p0.d
    cntp    x14, p1, p1.d
    cntp    x15, p2, p2.d
    cntp    x16, p3, p3.d
    mov     x17, #8
    mov     x18, #16
    mov     x19, #32
    mov     x20, #64
    cmp     x6,  x17
    csel    x17, x17, xzr, ge
    cmp     x6,  x18
    csel    x18, x18, xzr, ge
    cmp     x6,  x19
    csel    x19, x19, xzr, ge
    cmp     x6,  x20
    csel    x20, x20, xzr, ge
    cmp     x13, x17
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x18
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x19
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x20
    b.ne    exit
    add     x2,  x2, #1    // x2 = 0x30


exit: 
    # exit VADL virt (HTIF)
    mov     x0, #1           // cmd: exit 0
    ldr     x1, =tohost      // Load address of 'tohost' into x1
    str     x0, [x1]         // Store a0 to 'tohost'

    # exit upstream virt (semihosting)
    ldr     x1, =args
    mov     w0, #0x20        // SYS_EXIT operation
    hlt     #0xF000          // Trigger semihosting call

1:  b       1b               // Infinite loop

.section .data
.align 3
args: 
.xword 0x20026     // ADP_Stopped_ApplicationExit command
.xword 0x0         // Exit Code 

.section .tohost, "aw", @progbits
.align 6
.global tohost
tohost: .xword 0
.size tohost, 8

.align 6
.global fromhost
fromhost: .xword 0
.size fromhost, 8
