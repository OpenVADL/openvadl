# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -disassemble -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

# Encodings derived from sys/v-risc/VarRisc.vadl, imported by ABI_no_suffix.vadl.
# TYPE_R: rd[31:27] | rs1[26:22] | rs2[21:17] | scale[16:15] |
#         func9[14:6] | opcode[5:0], little-endian bytes.
# RegCode=63; Init=yes fixes scale=0. LLVM register IDs are R(n) + 2.

# ADD r0, r1, r31 (func9=0)
0x3f 0x00 0x7e 0x00
# CHECK: <MCInst #{{[0-9]+}} ADD{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:33>>

# SUB r3, r4, r5 (func9=1)
0x7f 0x00 0x0a 0x19
# CHECK: <MCInst #{{[0-9]+}} SUB{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>>

# MUL r2, r3, r4 (func9=2)
0xbf 0x00 0xc8 0x10
# CHECK: <MCInst #{{[0-9]+}} MUL{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>>

# DIV r10, r11, r12 (func9=3; divisor precedes dividend in the MC operands)
0xff 0x00 0xd8 0x52
# CHECK: <MCInst #{{[0-9]+}} DIV{{$}}
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>
# CHECK-NEXT: <MCOperand Reg:14>>

# SEQ r23, r24, r25 (func9=16)
0x3f 0x04 0x32 0xbe
# CHECK: <MCInst #{{[0-9]+}} SEQ{{$}}
# CHECK-NEXT: <MCOperand Reg:25>
# CHECK-NEXT: <MCOperand Reg:26>
# CHECK-NEXT: <MCOperand Reg:27>>

# ULT r31, r30, r29 (func9=20)
0x3f 0x05 0xba 0xff
# CHECK: <MCInst #{{[0-9]+}} ULT{{$}}
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Reg:32>
# CHECK-NEXT: <MCOperand Reg:31>>
