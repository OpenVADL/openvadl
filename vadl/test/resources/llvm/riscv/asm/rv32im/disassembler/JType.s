# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -disassemble -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

# JAL x16, 10
0x6f 0x08 0xa0 0x00
# CHECK: <MCInst #{{[0-9]+}} JAL{{$}}
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Imm:10>>

# JAL x16, -524288
0x6f 0x08 0x08 0x80
# CHECK: <MCInst #{{[0-9]+}} JAL{{$}}
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Imm:-524288>>

