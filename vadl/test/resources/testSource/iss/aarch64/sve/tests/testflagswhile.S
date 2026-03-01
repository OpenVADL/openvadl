# to communicate with the host
# check riscv-software-src/riscv-tests

.section .text.init
.global _start
_start:
    mov     x0, #((0b11<<20) | (0b11 << 16))
    msr     cpacr_el1, x0        // enable floating point and SVE
    mov     x2,  #0
    ptrue   p4.b
    ptrue   p5.h
    ptrue   p6.s
    ptrue   p7.d
    addvl   x3,  x2,  #1
    mov     x8,  #-2
    add     x9,  x8,  x3
    sub     x10, x9,  #1
    sub     x11, x9,  #2
    add     x12, x9,  #1
    whilelt p0.b,x8, x9
    mrs     x17, nzcv
    lsr     x17, x17, #28
    ptest   p4,p0.b
    mrs     x21, nzcv
    lsr     x21, x21, #28
    whilelt p1.b,x8, x10
    mrs     x18, nzcv
    lsr     x18, x18, #28
    ptest   p4,p1.b
    mrs     x22, nzcv
    lsr     x22, x22, #28
    whilelt p2.b,x8, x11
    mrs     x19, nzcv
    lsr     x19, x19, #28
    and     p8.b,p4/z,p2.b,p2.b
    mrs     x23, nzcv
    lsr     x23, x23, #28
    whilelt p3.b,x8, x12
    mrs     x20, nzcv
    lsr     x20, x20, #28
    orr     p8.b,p4/z,p3.b,p3.b
    mrs     x24, nzcv
    lsr     x24, x24, #28
    cmp     x17, #0x8
    bne     exit
    add     x2,  x2,  #1
    cmp     x17, x21
    bne     exit
    add     x2,  x2,  #1
    cmp     x18, #0xa
    bne     exit
    add     x2,  x2,  #1
    cmp     x18, x22
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x04
    cmp     x19, #0xa
    bne     exit
    add     x2,  x2,  #1
    cmp     x19, x23
    bne     exit
    add     x2,  x2,  #1
    cmp     x20, #0x8
    bne     exit
    add     x2,  x2,  #1
    cmp     x20, x24
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x08
    lsr     x4,  x3,  #1
    add     x9,  x8,  x4
    sub     x10, x9,  #1
    sub     x11, x9,  #2
    add     x12, x9,  #1
    whilelt p0.h,x8, x9
    mrs     x17, nzcv
    lsr     x17, x17, #28
    mov     p8.b,p0.b
    pnext   p8.h,p5,p8.h
    mrs     x21, nzcv
    lsr     x21, x21, #28
    whilelt p1.h,x8, x10
    mrs     x18, nzcv
    lsr     x18, x18, #28
    mov     p9.b,p1.b
    pnext   p9.h,p5,p9.h
    mrs     x22, nzcv
    lsr     x22, x22, #28
    whilelt p2.h,x8, x11
    mrs     x19, nzcv
    lsr     x19, x19, #28
    mov     p10.b,p2.b
    pnext   p10.h,p5,p10.h
    mrs     x23, nzcv
    lsr     x23, x23, #28
    whilelt p3.h,x8, x12
    mrs     x20, nzcv
    lsr     x20, x20, #28
    mov     p11.b,p3.b
    pnext   p11.h,p5,p11.h
    mrs     x24, nzcv
    lsr     x24, x24, #28
    cmp     x17, #0x8
    bne     exit
    add     x2,  x2,  #1
    cmp     x18, #0xa
    bne     exit
    add     x2,  x2,  #1
    cmp     x19, #0xa
    bne     exit
    add     x2,  x2,  #1
    cmp     x20, #0x8
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x0c
    cmp     x21, 0x6
    bne     exit
    add     x2,  x2,  #1
    cmp     x22, 0x0
    bne     exit
    add     x2,  x2,  #1
    cmp     x23, 0x2
    bne     exit
    add     x2,  x2,  #1
    cmp     x24, 0x6
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x10
    lsr     x4,  x3,  #2
    add     x9,  x8,  x4
    sub     x10, x9,  #1
    mov     x11, x8
    add     x12, x9,  #1
    whilelt p0.s,x8, x9
    mrs     x17, nzcv
    lsr     x17, x17, #28
    mov     p8.b,p0.b
    pfirst  p8.b,p5,p8.b
    mrs     x21, nzcv
    lsr     x21, x21, #28
    whilelt p1.s,x8, x10
    mrs     x18, nzcv
    lsr     x18, x18, #28
    mov     p9.b,p1.b
    pfirst  p9.b,p5,p9.b
    mrs     x22, nzcv
    lsr     x22, x22, #28
    whilelt p2.s,x8, x11
    mrs     x19, nzcv
    lsr     x19, x19, #28
    mov     p10.b,p2.b
    pfirst  p10.b,p5,p10.b
    mrs     x23, nzcv
    lsr     x23, x23, #28
    whilelt p3.s,x8, x12
    mrs     x20, nzcv
    lsr     x20, x20, #28
    mov     p11.b,p3.b
    pfirst  p11.b,p5,p11.b
    mrs     x24, nzcv
    lsr     x24, x24, #28
    cmp     x17, #0x8
    bne     exit
    add     x2,  x2,  #1
    cmp     x18, #0xa
    bne     exit
    add     x2,  x2,  #1
    cmp     x19, #0x6
    bne     exit
    add     x2,  x2,  #1
    cmp     x20, #0x8
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x14
    cmp     x21, 0xa
    bne     exit
    add     x2,  x2,  #1
    cmp     x22, 0xa
    bne     exit
    add     x2,  x2,  #1
    cmp     x23, 0xa
    bne     exit
    add     x2,  x2,  #1
    cmp     x24, 0xa
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x18
    mov     x8,  #-2
    add     x9,  x8,  x3
    sub     x10, x9,  #1
    sub     x11, x9,  #2
    add     x12, x9,  #1
    whilegt p0.b,x9,  x8
    mrs     x17, nzcv
    lsr     x17, x17, #28
    whilegt p1.b,x10, x8
    mrs     x18, nzcv
    lsr     x18, x18, #28
    whilegt p2.b,x11, x8
    mrs     x19, nzcv
    lsr     x19, x19, #28
    whilegt p3.b,x12, x8
    mrs     x20, nzcv
    lsr     x20, x20, #28
    cmp     x17, #0x8
    bne     exit
    add     x2,  x2,  #1
    cmp     x18, #0x0
    bne     exit
    add     x2,  x2,  #1
    cmp     x19, #0x0
    bne     exit
    add     x2,  x2,  #1
    cmp     x20, #0x8
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x1c
    lsr     x4,  x3,  #1
    add     x9,  x8,  x4
    sub     x10, x9,  #1
    sub     x11, x9,  #2
    add     x12, x9,  #1
    whilegt p0.h,x9,  x8
    mrs     x17, nzcv
    lsr     x17, x17, #28
    whilegt p1.h,x10, x8
    mrs     x18, nzcv
    lsr     x18, x18, #28
    whilegt p2.h,x11, x8
    mrs     x19, nzcv
    lsr     x19, x19, #28
    whilegt p3.h,x12, x8
    mrs     x20, nzcv
    lsr     x20, x20, #28
    cmp     x17, #0x8
    bne     exit
    add     x2,  x2,  #1
    cmp     x18, #0x0
    bne     exit
    add     x2,  x2,  #1
    cmp     x19, #0x0
    bne     exit
    add     x2,  x2,  #1
    cmp     x20, #0x8
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x20
    lsr     x4,  x3,  #2
    add     x9,  x8,  x4
    sub     x10, x9,  #1
    mov     x11, x8
    add     x12, x9,  #1
    whilegt p0.s,x9, x8
    mrs     x17, nzcv
    lsr     x17, x17, #28
    whilegt p1.s,x10, x8
    mrs     x18, nzcv
    lsr     x18, x18, #28
    whilegt p2.s,x11, x8
    mrs     x19, nzcv
    lsr     x19, x19, #28
    whilegt p3.s,x12, x8
    mrs     x20, nzcv
    lsr     x20, x20, #28
    cmp     x17, #0x8
    bne     exit
    add     x2,  x2,  #1
    cmp     x18, #0x0
    bne     exit
    add     x2,  x2,  #1
    cmp     x19, #0x6
    bne     exit
    add     x2,  x2,  #1
    cmp     x20, #0x8
    bne     exit
    add     x2,  x2,  #1     // x2 = 0x24

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
