# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

SLLI x0, x1, 2
# CHECK: <MCInst #{{[0-9]+}} SLLI
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:2>>

SRLI x2, x3, 3
# CHECK: <MCInst #{{[0-9]+}} SRLI
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:3>>

SRAI x4, x5, 4
# CHECK: <MCInst #{{[0-9]+}} SRAI
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Imm:4>>