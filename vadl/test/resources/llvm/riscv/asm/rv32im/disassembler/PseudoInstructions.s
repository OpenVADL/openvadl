# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -disassemble -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

# Pseudo instructions are disassembled into the machine instructions they expand to.

# RET -> JALR x0, x1, 0
0x67 0x80 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} JALR{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:0>>

# NOP -> ADDI x0, x0, 0
0x13 0x00 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} ADDI{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:0>>

# ECALL
0x73 0x00 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} ECALL>

# EBREAK
0x73 0x00 0x10 0x00
# CHECK: <MCInst #{{[0-9]+}} EBREAK>

# CALL my_function -> AUIPC x1, 0 ; JALR x1, x1, 0
0x97 0x00 0x00 0x00
0xe7 0x80 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} AUIPC{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:0>>
# CHECK: <MCInst #{{[0-9]+}} JALR{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:0>>

# TAIL my_function -> AUIPC x6, 0 ; JALR x0, x6, 0
0x17 0x03 0x00 0x00
0x67 0x00 0x03 0x00
# CHECK: <MCInst #{{[0-9]+}} AUIPC{{$}}
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:0>>
# CHECK: <MCInst #{{[0-9]+}} JALR{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:0>>

# J 100 -> JAL x0, 100
0x6f 0x00 0x40 0x06
# CHECK: <MCInst #{{[0-9]+}} JAL{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:100>>

# JR a5 -> JALR x0, x15, 0
0x67 0x80 0x07 0x00
# CHECK: <MCInst #{{[0-9]+}} JALR{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Imm:0>>

# MV x0, x1 -> ADDI x0, x1, 0
0x13 0x80 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} ADDI{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:0>>

# NOT x2, x3 -> XORI x2, x3, -1
0x13 0xc1 0xf1 0xff
# CHECK: <MCInst #{{[0-9]+}} XORI{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:-1>>

# NEG x4, x5 -> SUB x4, x0, x5
0x33 0x02 0x50 0x40
# CHECK: <MCInst #{{[0-9]+}} SUB{{$}}
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:7>>

# SNEZ x6, x7 -> SLTU x6, x0, x7
0x33 0x33 0x70 0x00
# CHECK: <MCInst #{{[0-9]+}} SLTU{{$}}
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:9>>

# SLTZ x8, x9 -> SLT x8, x9, x0
0x33 0xa4 0x04 0x00
# CHECK: <MCInst #{{[0-9]+}} SLT{{$}}
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:2>>

# SGTZ x10, x11 -> SLT x10, x0, x11
0x33 0x25 0xb0 0x00
# CHECK: <MCInst #{{[0-9]+}} SLT{{$}}
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:13>>

# BEQZ x1, 1 -> BEQ x1, x0, 0 (lowest bit is not encodable)
0x63 0x80 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} BEQ{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:0>>

# BNEZ x2, 2 -> BNE x2, x0, 2
0x63 0x11 0x01 0x00
# CHECK: <MCInst #{{[0-9]+}} BNE{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:2>>

# BLEZ x3, 3 -> BGE x0, x3, 2 (lowest bit is not encodable)
0x63 0x51 0x30 0x00
# CHECK: <MCInst #{{[0-9]+}} BGE{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:2>>

# BGEZ x4, 4 -> BGE x4, x0, 4
0x63 0x52 0x02 0x00
# CHECK: <MCInst #{{[0-9]+}} BGE{{$}}
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:4>>

# BGEZ x4, .lbl -> BGE x4, x0, 0 (unresolved fixup)
0x63 0x50 0x02 0x00
# CHECK: <MCInst #{{[0-9]+}} BGE{{$}}
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:0>>

# BLTZ x5, 5 -> BLT x5, x0, 4 (lowest bit is not encodable)
0x63 0xc2 0x02 0x00
# CHECK: <MCInst #{{[0-9]+}} BLT{{$}}
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:4>>

# BGTZ x6, 6 -> BLT x0, x6, 6
0x63 0x43 0x60 0x00
# CHECK: <MCInst #{{[0-9]+}} BLT{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:6>>

# LLA x3, .lbl -> AUIPC x3, 0 ; ADDI x3, x3, 0 (unresolved fixups)
0x97 0x01 0x00 0x00
0x93 0x81 0x01 0x00
# CHECK: <MCInst #{{[0-9]+}} AUIPC{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:0>>
# CHECK: <MCInst #{{[0-9]+}} ADDI{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:0>>

# LI x3, 0x12345 -> LUI x3, 0x12 ; ADDI x3, x3, 0x345
0xb7 0x21 0x01 0x00
0x93 0x81 0x51 0x34
# CHECK: <MCInst #{{[0-9]+}} LUI{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:73728>>
# CHECK: <MCInst #{{[0-9]+}} ADDI{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:837>>

# LGA x3, my_label -> AUIPC x3, 0 ; LW x3, 0(x3) (unresolved fixups)
0x97 0x01 0x00 0x00
0x83 0xa1 0x01 0x00
# CHECK: <MCInst #{{[0-9]+}} AUIPC{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:0>>
# CHECK: <MCInst #{{[0-9]+}} LW{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:0>>

