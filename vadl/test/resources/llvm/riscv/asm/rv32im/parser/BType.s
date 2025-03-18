# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

BEQ x1, x2, 1
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2>>

BNE x3, x4, 2
# CHECK: <MCInst #{{[0-9]+}} BNE
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Imm:4>>

BGE x5, x6, 3
# CHECK: <MCInst #{{[0-9]+}} BGE
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:6>>

BGEU x7, x8, 4
# CHECK: <MCInst #{{[0-9]+}} BGEU
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Imm:8>>

BLT x9, x10, 5
# CHECK: <MCInst #{{[0-9]+}} BLT
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Imm:10>>

BLTU x11, x12, 6
# CHECK: <MCInst #{{[0-9]+}} BLTU
# CHECK-NEXT: <MCOperand Reg:13>
# CHECK-NEXT: <MCOperand Reg:14>
# CHECK-NEXT: <MCOperand Imm:12>>