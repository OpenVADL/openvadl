# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

LUI x1, %hi(0xFFFF)
# CHECK: <MCInst #{{[0-9]+}} LUI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(16)>>

LUI x1, 0xF
# CHECK: <MCInst #{{[0-9]+}} LUI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:61440>>

AUIPC x2, 10
# CHECK: <MCInst #{{[0-9]+}} AUIPC
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:40960>>