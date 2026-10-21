# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -disassemble -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT
# SLLI x0, x1, 2
0x13 0x90 0x20 0x00
# CHECK: <MCInst #{{[0-9]+}} SLLI{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:2>>

# SRLI x2, x3, 3
0x13 0xd1 0x31 0x00
# CHECK: <MCInst #{{[0-9]+}} SRLI{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:3>>

# SRAI x4, x5, 4
0x13 0xd2 0x42 0x40
# CHECK: <MCInst #{{[0-9]+}} SRAI{{$}}
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Imm:4>>
