# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -disassemble -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

# Encodings derived from sys/v-risc/VarRisc.vadl, imported by ABI_no_suffix.vadl.
# TYPE_L: imm[47:16] | func5/rs1[15:11] | rd[10:6] | opcode[5:0].
# Bytes are little-endian; LLVM register IDs are R(n) + 2.
# Unlike TYPE_B/I/C, long branch offsets are not shifted.

# ADDI r1, r2, 305419896 (ADDI_L, opcode=ImmLOff+0=52, imm=0x12345678)
0x74 0x10 0x78 0x56 0x34 0x12
# CHECK: <MCInst #{{[0-9]+}} ADDI_L{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:305419896>>

# ADDI r3, r4, -2147483648 (ADDI_L, signed 32-bit minimum)
0xf4 0x20 0x00 0x00 0x00 0x80
# CHECK: <MCInst #{{[0-9]+}} ADDI_L{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Imm:-2147483648>>

# REMI r7, r8, 4660 (REMI_L, opcode=ImmLOff+4=56)
0xf8 0x41 0x34 0x12 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} REMI_L{{$}}
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Imm:4660>>

# XORI r31, r0, -1025 (XORI_L, opcode=ImmLOff+7=59)
0xfb 0x07 0xff 0xfb 0xff 0xff
# CHECK: <MCInst #{{[0-9]+}} XORI_L{{$}}
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:-1025>>

# LDW r20, 74565(r21) (LDW_L, opcode=MemLOff+2=34)
0x22 0xad 0x45 0x23 0x01 0x00
# CHECK: <MCInst #{{[0-9]+}} LDW_L{{$}}
# CHECK-NEXT: <MCOperand Reg:22>
# CHECK-NEXT: <MCOperand Reg:23>
# CHECK-NEXT: <MCOperand Imm:74565>>

# STW r22, 305419896(r23) (STW_L, opcode=MemLOff+5=37)
0xa5 0xbd 0x78 0x56 0x34 0x12
# CHECK: <MCInst #{{[0-9]+}} STW_L{{$}}
# CHECK-NEXT: <MCOperand Reg:25>
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Imm:305419896>>

# BEQ r14, r15, -65536 (BEQ_L, opcode=BraLOff+16=46)
0xae 0x7b 0x00 0x00 0xff 0xff
# CHECK: <MCInst #{{[0-9]+}} BEQ_L{{$}}
# CHECK-NEXT: <MCOperand Reg:16>
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Imm:-65536>>

# BULE r16, r17, 2147483647 (BULE_L, opcode=BraLOff+21=51)
0x33 0x8c 0xff 0xff 0xff 0x7f
# CHECK: <MCInst #{{[0-9]+}} BULE_L{{$}}
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:19>
# CHECK-NEXT: <MCOperand Imm:2147483647>>

# SGTI r26, -1025 (SGTI_L, opcode=CodeI_L=60, func5=22; tied rd)
0xbc 0xb6 0xff 0xfb 0xff 0xff
# CHECK: <MCInst #{{[0-9]+}} SGTI_L{{$}}
# CHECK-NEXT: <MCOperand Reg:28>
# CHECK-NEXT: <MCOperand Reg:28>
# CHECK-NEXT: <MCOperand Imm:-1025>>

# UGEI r28, 2147483647 (UGEI_L, opcode=CodeI_L=60, func5=25; tied rd)
0x3c 0xcf 0xff 0xff 0xff 0x7f
# CHECK: <MCInst #{{[0-9]+}} UGEI_L{{$}}
# CHECK-NEXT: <MCOperand Reg:30>
# CHECK-NEXT: <MCOperand Reg:30>
# CHECK-NEXT: <MCOperand Imm:2147483647>>

# BAL r31, -2147483648 (BAL_L, opcode=CodeI_L=60, func5=31)
0xfc 0xff 0x00 0x00 0x00 0x80
# CHECK: <MCInst #{{[0-9]+}} BAL_L{{$}}
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Imm:-2147483648>>

# BAL r22, 2147483647 (BAL_L, signed 32-bit maximum)
0xbc 0xfd 0xff 0xff 0xff 0x7f
# CHECK: <MCInst #{{[0-9]+}} BAL_L{{$}}
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Imm:2147483647>>
