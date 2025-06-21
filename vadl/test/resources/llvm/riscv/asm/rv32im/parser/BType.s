# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

BNE x3, x4, 2
# CHECK: <MCInst #{{[0-9]+}} BNE
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Imm:2>>

BGEU x7, x8, 4
# CHECK: <MCInst #{{[0-9]+}} BGEU
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Imm:4>>

BLTU x11, x12, 6
# CHECK: <MCInst #{{[0-9]+}} BLTU
# CHECK-NEXT: <MCOperand Reg:13>
# CHECK-NEXT: <MCOperand Reg:14>
# CHECK-NEXT: <MCOperand Imm:6>>