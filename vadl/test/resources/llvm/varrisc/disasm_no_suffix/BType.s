# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -disassemble -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

# Encodings derived from sys/v-risc/VarRisc.vadl, imported by ABI_no_suffix.vadl.
# TYPE_B: offset[15:6] | opcode[5:0], little-endian bytes; BRA_S opcode=23.
# MC immediate = sign_extend(offset, 10) << 1.

# BRA 0 (BRA_S)
0x17 0x00
# CHECK: <MCInst #{{[0-9]+}} BRA_S{{$}}
# CHECK-NEXT: <MCOperand Imm:0>>

# BRA 511 (BRA_S, signed 10-bit maximum; byte offset=1022)
0xd7 0x7f
# CHECK: <MCInst #{{[0-9]+}} BRA_S{{$}}
# CHECK-NEXT: <MCOperand Imm:1022>>

# BRA -512 (BRA_S, signed 10-bit minimum; byte offset=-1024)
0x17 0x80
# CHECK: <MCInst #{{[0-9]+}} BRA_S{{$}}
# CHECK-NEXT: <MCOperand Imm:-1024>>

# BRA -1 (BRA_S; byte offset=-2)
0xd7 0xff
# CHECK: <MCInst #{{[0-9]+}} BRA_S{{$}}
# CHECK-NEXT: <MCOperand Imm:-2>>
