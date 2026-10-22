# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -disassemble -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

# Encodings derived from sys/v-risc/VarRisc.vadl, imported by ABI_no_suffix.vadl.
# One continuous byte stream: TYPE_S (2), TYPE_L (6), TYPE_R (4), TYPE_B (2).
# Keeping all bytes on one line verifies that decoding advances by the actual
# instruction size, including a final short instruction with only two bytes left.
# ADD r0,r31; ADDI r1,r2,305419896; MUL r2,r3,r4; BRA -1.
0x00 0xf8 0x74 0x10 0x78 0x56 0x34 0x12 0xbf 0x00 0xc8 0x10 0xd7 0xff
# CHECK: <MCInst #{{[0-9]+}} ADD_S{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:33>>
# CHECK: <MCInst #{{[0-9]+}} ADDI_L{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:305419896>>
# CHECK: <MCInst #{{[0-9]+}} MUL{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>>
# CHECK: <MCInst #{{[0-9]+}} BRA_S{{$}}
# CHECK-NEXT: <MCOperand Imm:-2>>
