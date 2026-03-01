# to communicate with the host
# check riscv-software-src/riscv-tests

.section .text.init
.global _start
_start:
    mov     x0, #((0b11<<20) | (0b11 << 16))
    msr     cpacr_el1, x0        // enable floating point and SVE
    mov     x2,  #0
    mov     x3,  #0
    mov     x4,  #1
    mov     x5,  #2
    mov     x8,  #-32
    add     x9,  x8,  #1
    add     x10, x8,  #2
    sub     x11, x8,  #1
    whilelt p0.b,x8, x8
    whilelt p1.b,x8, x9
    whilelt p2.b,x8, x10
    whilelt p3.b,x8, x11
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
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilele p0.b,x8, x8
    whilele p1.b,x8, x9
    whilele p2.b,x8, x9
    whilele p3.b,x8, x11
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilegt p0.b,x8, x8
    whilegt p1.b,x8, x9
    whilegt p2.b,x8, x10
    whilegt p3.b,x8, x11
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x4
    b.ne    exit
    add     x2,  x2, #1
    whilege p0.b,x8, x8
    whilege p1.b,x8, x9
    whilege p2.b,x8, x10
    whilege p3.b,x8, x11
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x5
    b.ne    exit
    add     x2,  x2, #1
    whilelo p0.b,x8, x8
    whilelo p1.b,x8, x9
    whilelo p2.b,x8, x10
    whilelo p3.b,x8, x11
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
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilels p0.b,x8, x8
    whilels p1.b,x8, x9
    whilels p2.b,x8, x9
    whilels p3.b,x8, x11
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilehi p0.b,x8, x8
    whilehi p1.b,x8, x9
    whilehi p2.b,x8, x10
    whilehi p3.b,x8, x11
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x4
    b.ne    exit
    add     x2,  x2, #1
    whilehs p0.b,x8, x8
    whilehs p1.b,x8, x9
    whilehs p2.b,x8, x10
    whilehs p3.b,x8, x11
    cntp    x13, p0, p0.b
    cntp    x14, p1, p1.b
    cntp    x15, p2, p2.b
    cntp    x16, p3, p3.b
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x5
    b.ne    exit
    add     x2,  x2, #1
    whilelt p0.h,x8, x8
    whilelt p1.h,x8, x9
    whilelt p2.h,x8, x10
    whilelt p3.h,x8, x11
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilele p0.h,x8, x8
    whilele p1.h,x8, x9
    whilele p2.h,x8, x9
    whilele p3.h,x8, x11
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilegt p0.h,x8, x8
    whilegt p1.h,x8, x9
    whilegt p2.h,x8, x10
    whilegt p3.h,x8, x11
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x4
    b.ne    exit
    add     x2,  x2, #1
    whilege p0.h,x8, x8
    whilege p1.h,x8, x9
    whilege p2.h,x8, x10
    whilege p3.h,x8, x11
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x5
    b.ne    exit
    add     x2,  x2, #1
    whilelo p0.h,x8, x8
    whilelo p1.h,x8, x9
    whilelo p2.h,x8, x10
    whilelo p3.h,x8, x11
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilels p0.h,x8, x8
    whilels p1.h,x8, x9
    whilels p2.h,x8, x9
    whilels p3.h,x8, x11
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilehi p0.h,x8, x8
    whilehi p1.h,x8, x9
    whilehi p2.h,x8, x10
    whilehi p3.h,x8, x11
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x4
    b.ne    exit
    add     x2,  x2, #1
    whilehs p0.h,x8, x8
    whilehs p1.h,x8, x9
    whilehs p2.h,x8, x10
    whilehs p3.h,x8, x11
    cntp    x13, p0, p0.h
    cntp    x14, p1, p1.h
    cntp    x15, p2, p2.h
    cntp    x16, p3, p3.h
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x5
    b.ne    exit
    add     x2,  x2, #1
    whilelt p0.s,x8, x8
    whilelt p1.s,x8, x9
    whilelt p2.s,x8, x10
    whilelt p3.s,x8, x11
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilele p0.s,x8, x8
    whilele p1.s,x8, x9
    whilele p2.s,x8, x9
    whilele p3.s,x8, x11
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilegt p0.s,x8, x8
    whilegt p1.s,x8, x9
    whilegt p2.s,x8, x10
    whilegt p3.s,x8, x11
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x4
    b.ne    exit
    add     x2,  x2, #1
    whilege p0.s,x8, x8
    whilege p1.s,x8, x9
    whilege p2.s,x8, x10
    whilege p3.s,x8, x11
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x5
    b.ne    exit
    add     x2,  x2, #1
    whilelo p0.s,x8, x8
    whilelo p1.s,x8, x9
    whilelo p2.s,x8, x10
    whilelo p3.s,x8, x11
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilels p0.s,x8, x8
    whilels p1.s,x8, x9
    whilels p2.s,x8, x9
    whilels p3.s,x8, x11
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x5
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1
    whilehi p0.s,x8, x8
    whilehi p1.s,x8, x9
    whilehi p2.s,x8, x10
    whilehi p3.s,x8, x11
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x4
    b.ne    exit
    add     x2,  x2, #1
    whilehs p0.s,x8, x8
    whilehs p1.s,x8, x9
    whilehs p2.s,x8, x10
    whilehs p3.s,x8, x11
    cntp    x13, p0, p0.s
    cntp    x14, p1, p1.s
    cntp    x15, p2, p2.s
    cntp    x16, p3, p3.s
    cmp     x13, x4
    b.ne    exit
    add     x2,  x2, #1
    cmp     x14, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x15, x3
    b.ne    exit
    add     x2,  x2, #1
    cmp     x16, x5
    b.ne    exit
    add     x2,  x2, #1


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
