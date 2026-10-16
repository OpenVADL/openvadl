# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -disassemble -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

# ADD x0, x1, x2
0x33 0x80 0x20 0x00
# CHECK: <MCInst #{{[0-9]+}} ADD{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>>

# SUB x3, x4, x5
0xb3 0x01 0x52 0x40
# CHECK: <MCInst #{{[0-9]+}} SUB{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>>

# AND x6, x7, x8
0x33 0xf3 0x83 0x00
# CHECK: <MCInst #{{[0-9]+}} AND{{$}}
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>>

# OR x9, x10, x11
0xb3 0x64 0xb5 0x00
# CHECK: <MCInst #{{[0-9]+}} OR{{$}}
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>>

# XOR x12, x13, x14
0x33 0xc6 0xe6 0x00
# CHECK: <MCInst #{{[0-9]+}} XOR{{$}}
# CHECK-NEXT: <MCOperand Reg:14>
# CHECK-NEXT: <MCOperand Reg:15>
# CHECK-NEXT: <MCOperand Reg:16>>

# SLT x15, x16, x17
0xb3 0x27 0x18 0x01
# CHECK: <MCInst #{{[0-9]+}} SLT{{$}}
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:19>>

# SLTU x18, x19, x20
0x33 0xb9 0x49 0x01
# CHECK: <MCInst #{{[0-9]+}} SLTU{{$}}
# CHECK-NEXT: <MCOperand Reg:20>
# CHECK-NEXT: <MCOperand Reg:21>
# CHECK-NEXT: <MCOperand Reg:22>>

# SLL x21, x22, x23
0xb3 0x1a 0x7b 0x01
# CHECK: <MCInst #{{[0-9]+}} SLL{{$}}
# CHECK-NEXT: <MCOperand Reg:23>
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Reg:25>>

# SRL x24, x25, x26
0x33 0xdc 0xac 0x01
# CHECK: <MCInst #{{[0-9]+}} SRL{{$}}
# CHECK-NEXT: <MCOperand Reg:26>
# CHECK-NEXT: <MCOperand Reg:27>
# CHECK-NEXT: <MCOperand Reg:28>>

# SRA x27, x28, x29
0xb3 0x5d 0xde 0x41
# CHECK: <MCInst #{{[0-9]+}} SRA{{$}}
# CHECK-NEXT: <MCOperand Reg:29>
# CHECK-NEXT: <MCOperand Reg:30>
# CHECK-NEXT: <MCOperand Reg:31>>

# MUL x30, x31, x8
0x33 0x8f 0x8f 0x02
# CHECK: <MCInst #{{[0-9]+}} MUL{{$}}
# CHECK-NEXT: <MCOperand Reg:32>
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Reg:10>>

# MULH x4, x5, x6
0x33 0x92 0x62 0x02
# CHECK: <MCInst #{{[0-9]+}} MULH{{$}}
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Reg:8>>

# MULHSU x7, x8, x9
0xb3 0x23 0x94 0x02
# CHECK: <MCInst #{{[0-9]+}} MULHSU{{$}}
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>>

# MULHU x10, x11, x12
0x33 0xb5 0xc5 0x02
# CHECK: <MCInst #{{[0-9]+}} MULHU{{$}}
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>
# CHECK-NEXT: <MCOperand Reg:14>>

# DIV x13, x14, x15
0xb3 0x46 0xf7 0x02
# CHECK: <MCInst #{{[0-9]+}} DIV{{$}}
# CHECK-NEXT: <MCOperand Reg:15>
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Reg:16>>

# DIVU x16, x17, x18
0x33 0xd8 0x28 0x03
# CHECK: <MCInst #{{[0-9]+}} DIVU{{$}}
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:20>
# CHECK-NEXT: <MCOperand Reg:19>>

# REM x19, x20, x21
0xb3 0x69 0x5a 0x03
# CHECK: <MCInst #{{[0-9]+}} REM{{$}}
# CHECK-NEXT: <MCOperand Reg:21>
# CHECK-NEXT: <MCOperand Reg:23>
# CHECK-NEXT: <MCOperand Reg:22>>

# REMU x22, x23, x24
0x33 0xfb 0x8b 0x03
# CHECK: <MCInst #{{[0-9]+}} REMU{{$}}
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Reg:26>
# CHECK-NEXT: <MCOperand Reg:25>>

