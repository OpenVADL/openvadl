# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

LB x0, 10(x1)
# CHECK: <MCInst #{{[0-9]+}} LB
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:10>>

LBU x2, 15(x3)
# CHECK: <MCInst #{{[0-9]+}} LBU
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:15>>

LH x4, 20(x5)
# CHECK: <MCInst #{{[0-9]+}} LH
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Imm:20>>

LHU x6, 25(x7)
# CHECK: <MCInst #{{[0-9]+}} LHU
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Imm:25>>

LW x8, 30(x9)
# CHECK: <MCInst #{{[0-9]+}} LW
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Imm:30>>