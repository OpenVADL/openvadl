# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -disassemble -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

# Encodings derived from sys/v-risc/VarRisc.vadl, imported by ABI_no_suffix.vadl.
# TYPE_C: rd[31:27] | imm[26:11] | func5[10:6] | opcode[5:0], little-endian.
# BAL: opcode=ImmCode=62, func5=31; MC immediate = sign_extend(imm, 16) << 1.

# BAL r1, 0
0xfe 0x07 0x00 0x08
# CHECK: <MCInst #{{[0-9]+}} BAL{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:0>>

# BAL r31, 32767 (signed 16-bit maximum; byte offset=65534)
0xfe 0xff 0xff 0xfb
# CHECK: <MCInst #{{[0-9]+}} BAL{{$}}
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Imm:65534>>

# BAL r22, -32768 (signed 16-bit minimum; byte offset=-65536)
0xfe 0x07 0x00 0xb4
# CHECK: <MCInst #{{[0-9]+}} BAL{{$}}
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Imm:-65536>>

# BAL r3, -1 (byte offset=-2)
0xfe 0xff 0xff 0x1f
# CHECK: <MCInst #{{[0-9]+}} BAL{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:-2>>
