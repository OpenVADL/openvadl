# to communicate with the host
# check riscv-software-src/riscv-tests

.section .text.init
.global _start
_start:

    mov     x0, #((0b11<<20) | (0b11 << 16))
    msr     cpacr_el1, x0        // enable floating point and SVE

    mov     x2,  #0

    ldr     x0,  =vectora
    mov     x1,  #7
    bl      vsumb
    mov     x10, x0
    cmp     x10, #0x15
    b.ne    exit
    add     x2,  x2, #1

    ldr     x0,  =vectora
    mov     x1,  #13
    bl      vsumb
    mov     x11, x0
    cmp     x11, #0x4e
    b.ne    exit
    add     x2,  x2, #1      // x2 = 0x2

    ldr     x0,  =vectora
    mov     x1,  #64
    bl      vsumb
    mov     x12, x0
    mov     x3,  #0x7e0
    cmp     x12, x3
    b.ne    exit
    add     x2,  x2, #1

    ldr     x0,  =vectora
    mov     x1,  #125
    bl      vsumb
    mov     x13, x0
    mov     x3,  #0x1e46
    cmp     x13, x3
    b.ne    exit
    add     x2,  x2, #1      // x2 = 0x4

    ldr     x0,  =vectora
    mov     x1,  #7
    bl      vaddb
    mov     x14, x0
    cmp     x14, #0x2a
    b.ne    exit
    add     x2,  x2, #1

    ldr     x0,  =vectora
    mov     x1,  #13
    bl      vaddb
    mov     x15, x0
    cmp     x15, #0x9c
    b.ne    exit
    add     x2,  x2, #1      // x2 = 0x6

    ldr     x0,  =vectora
    mov     x1,  #64
    bl      vaddb
    mov     x16, x0
    mov     x3,  #0xfc0
    cmp     x16, x3
    b.ne    exit
    add     x2,  x2, #1

    ldr     x0,  =vectora
    mov     x1,  #125
    bl      vaddb
    mov     x17, x0
    mov     x3,  #0x3c8c
    cmp     x17, x3
    b.ne    exit
    add     x2,  x2, #1      // x2 = 0x8

    ldr     x0,  =vectora
    mov     x1,  #3
    bl      vsumh
    mov     x10, x0
    mov     x3,  #0x906
    cmp     x10, x3
    b.ne    exit
    add     x2,  x2, #1

    ldr     x0,  =vectora
    mov     x1,  #7
    bl      vsumh
    mov     x10, x0
    mov     x3,  #0x312a
    cmp     x10, x3
    b.ne    exit
    add     x2,  x2, #1

    ldr     x0,  =vectora
    mov     x1,  #13
    bl      vsumh
    mov     x10, x0
    mov     x3,  #0xa99c
    cmp     x10, x3
    b.ne    exit
    add     x2,  x2, #1

    ldr     x0,  =vectora
    mov     x1,  #32
    bl      vsumh
    mov     x10, x0
    mov     x3,  #0x03e0
    movk    x3,  #0x4,lsl 16
    cmp     x10, x3
    b.ne    exit
    add     x2,  x2, #1      // x2 = 0xc


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

vsumb:                       // vector sum, x0 = address, x1 = number of elements
    add     x1, x0, x1       // uses p0, z0, v1, x8, x9
    mov     x8, #0
    whilelo p0.b, x0, x1
loopvsumb:
    mov     p1.b, p0.b
    ld1b    z0.b, p0/z, [x0]
    uaddv   d1,  p0, z0.b
    umov    x9, v1.d[0]
    add     x8, x8, x9
    incb    x0
    whilelo p0.b, x0, x1
    b.mi    loopvsumb
    mov     x0, x8           // returns vector sum in x0
    ret

vaddb:                       // vector add, x0 = address, x1 = number of elements
    add     x1, x0, x1       // uses p0, z0, v1, x8, x9
    mov     x8, #0
    whilelo p0.b, x0, x1
loopvaddb:
    mov     p1.b, p0.b
    ld1b    z0.b, p0/z, [x0]
    add     z0.b, z0.b, z0.b
    uaddv   d1,  p0, z0.b
    umov    x9, v1.d[0]
    add     x8, x8, x9
    incb    x0
    whilelo p0.b, x0, x1
    b.mi    loopvaddb
    mov     x0, x8           // returns vector sum in x0
    ret

vsumh:                       // vector sum, x0 = address, x1 = number of elements
    mov     x8, #0           // uses p0, z0, v1, v4, x3, x6, x7, x8, x9
    mov     x3, #0
    whilelo p0.h, x3, x1
loopvsumh:
    mov     p3.b, p2.b
    mov     p2.b, p1.b
    mov     p1.b, p0.b
    sub     z4.b, z4.b, z4.b
    add     z4.b, z4.b, z0.b
    ld1h    z0.h, p0/z, [x0, x3, lsl #1]
    uaddv   d1,  p0, z0.h
    umov    x9, v1.d[0]
    add     x8, x8, x9
    mov     x5, x0
    inch    x3
    whilelo p0.h, x3, x1
    b.mi    loopvsumh
    mov     x6, x0
    mov     x7, x1
    mov     x0, x8           // returns vector sum in x0
    ret

vaddh:                       // vector add, x0 = address, x1 = number of elements
    add     x1, x0, x1       // uses p0, z0, v1, x8, x9
    mov     x8, #0
    whilelo p0.h, x0, x1
loopvaddh:
    mov     p1.b, p0.b
    ld1h    z0.h, p0/z, [x0]
    add     z0.h, z0.h, z0.h
    uaddv   d1,  p0, z0.h
    umov    x9, v1.d[0]
    add     x8, x8, x9
    inch    x0
    whilelo p0.h, x0, x1
    b.mi    loopvaddh
    mov     x0, x8           // returns vector sum in x0
    ret


.section .data
.align 3
args: 
.xword 0x20026     // ADP_Stopped_ApplicationExit command
.xword 0x0         // Exit Code 

.align 5
vectora:
.byte 0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07, 0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f
.byte 0x10,0x11,0x12,0x13,0x14,0x15,0x16,0x17, 0x18,0x19,0x1a,0x1b,0x1c,0x1d,0x1e,0x1f
.byte 0x20,0x21,0x22,0x23,0x24,0x25,0x26,0x27, 0x28,0x29,0x2a,0x2b,0x2c,0x2d,0x2e,0x2f
.byte 0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37, 0x38,0x39,0x3a,0x3b,0x3c,0x3d,0x3e,0x3f
.byte 0x40,0x41,0x42,0x43,0x44,0x45,0x46,0x47, 0x48,0x49,0x4a,0x4b,0x4c,0x4d,0x4e,0x4f
.byte 0x50,0x51,0x52,0x53,0x54,0x55,0x56,0x57, 0x58,0x59,0x5a,0x5b,0x5c,0x5d,0x5e,0x5f
.byte 0x60,0x61,0x62,0x63,0x64,0x65,0x66,0x67, 0x68,0x69,0x6a,0x6b,0x6c,0x6d,0x6e,0x6f
.byte 0x70,0x71,0x72,0x73,0x74,0x75,0x76,0x77, 0x78,0x79,0x7a,0x7b,0x7c,0x7d,0x7e,0x7f
.byte 0x80,0x81,0x82,0x83,0x84,0x85,0x86,0x87, 0x88,0x89,0x8a,0x8b,0x8c,0x8d,0x8e,0x8f
.byte 0x90,0x91,0x92,0x93,0x94,0x95,0x96,0x97, 0x98,0x99,0x9a,0x9b,0x9c,0x9d,0x9e,0x9f
.byte 0xa0,0xa1,0xa2,0xa3,0xa4,0xa5,0xa6,0xa7, 0xa8,0xa9,0xaa,0xab,0xac,0xad,0xae,0xaf
.byte 0xb0,0xb1,0xb2,0xb3,0xb4,0xb5,0xb6,0xb7, 0xb8,0xb9,0xba,0xbb,0xbc,0xbd,0xbe,0xbf
.byte 0xc0,0xc1,0xc2,0xc3,0xc4,0xc5,0xc6,0xc7, 0xc8,0xc9,0xca,0xcb,0xcc,0xcd,0xce,0xcf
.byte 0xd0,0xd1,0xd2,0xd3,0xd4,0xd5,0xd6,0xd7, 0xd8,0xd9,0xda,0xdb,0xdc,0xdd,0xde,0xdf
.byte 0xe0,0xe1,0xe2,0xe3,0xe4,0xe5,0xe6,0xe7, 0xe8,0xe9,0xea,0xeb,0xec,0xed,0xee,0xef
.byte 0xf0,0xf1,0xf2,0xf3,0xf4,0xf5,0xf6,0xf7, 0xf8,0xf9,0xfa,0xfb,0xfc,0xfd,0xfe,0xff

.section .tohost, "aw", @progbits
.align 6
.global tohost
tohost: .xword 0
.size tohost, 8

.align 6
.global fromhost
fromhost: .xword 0
.size fromhost, 8
