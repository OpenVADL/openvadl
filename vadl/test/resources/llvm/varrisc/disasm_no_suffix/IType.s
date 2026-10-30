# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -disassemble -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

# Encodings derived from sys/v-risc/VarRisc.vadl, imported by ABI_no_suffix.vadl.
# TYPE_I: rd[31:27] | rs1[26:22] | imm[21:11] | func5[10:6] | opcode[5:0].
# Bytes are little-endian; ImmCode=62, MemCode=61.
# Branch MC immediates are sign_extend(imm, 11) << 1.

# ADDI r0, r31, 0 (opcode=62, func5=0)
0x3e 0x00 0xc0 0x07
# CHECK: <MCInst #{{[0-9]+}} ADDI{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Imm:0>>

# ADDI r1, r2, 1023 (signed 11-bit maximum)
0x3e 0xf8 0x9f 0x08
# CHECK: <MCInst #{{[0-9]+}} ADDI{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1023>>

# ADDI r3, r4, -1024 (printed via sdec; but inst behavior uses raw imm field)
0x3e 0x00 0x20 0x19
# CHECK: ADDI gp,tp,-1024
# CHECK-SAME: <MCInst #{{[0-9]+}} ADDI{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Imm:1024>>

# DIVI r5, r6, 255 (opcode=62, func5=3)
0xfe 0xf8 0x87 0x29
# CHECK: <MCInst #{{[0-9]+}} DIVI{{$}}
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:255>>

# ASRI r7, r8, 31 (opcode=62, func5=8)
0x3e 0xfa 0x00 0x3a
# CHECK: <MCInst #{{[0-9]+}} ASRI{{$}}
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Imm:31>>

# SEQI r9, r10, -1 (opcode=62, func5=16; operand is the field access function immS, which is sign-extended)
0x3e 0xfc 0xbf 0x4a
# CHECK: <MCInst #{{[0-9]+}} SEQI{{$}}
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Imm:-1>>

# LDB r16, 1(r17) (opcode=61, func5=0)
0x3d 0x08 0x40 0x84
# CHECK: <MCInst #{{[0-9]+}} LDB{{$}}
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:19>
# CHECK-NEXT: <MCOperand Imm:1>>

# STW r18, 1023(r19) (opcode=61, func5=5; base precedes stored value)
0x7d 0xf9 0xdf 0x94
# CHECK: <MCInst #{{[0-9]+}} STW{{$}}
# CHECK-NEXT: <MCOperand Reg:21>
# CHECK-NEXT: <MCOperand Reg:20>
# CHECK-NEXT: <MCOperand Imm:1023>>

# BEQ r1, r2, 1 (opcode=61, func5=16; byte offset=2)
0x3d 0x0c 0x80 0x08
# CHECK: <MCInst #{{[0-9]+}} BEQ{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2>>

# BNE r3, r4, -1024 (opcode=61, func5=17; byte offset=-2048)
0x7d 0x04 0x20 0x19
# CHECK: <MCInst #{{[0-9]+}} BNE{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Imm:-2048>>

# BULT r5, r6, 1023 (opcode=61, func5=20; byte offset=2046)
0x3d 0xfd 0x9f 0x29
# CHECK: <MCInst #{{[0-9]+}} BULT{{$}}
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:2046>>
