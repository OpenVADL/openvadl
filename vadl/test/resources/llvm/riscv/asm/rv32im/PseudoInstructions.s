# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT


RET
# CHECK: <MCInst #{{[0-9]+}} RET>

NOP
# CHECK: <MCInst #{{[0-9]+}} NOP>

ECALL
# CHECK: <MCInst #{{[0-9]+}} ECALL>

EBREAK
# CHECK: <MCInst #{{[0-9]+}} EBREAK>


CALL my_function
# CHECK: <MCInst #{{[0-9]+}} CALL
# CHECK-NEXT: <MCOperand Expr:(my_function)>

TAIL my_function
# CHECK: <MCInst #{{[0-9]+}} TAIL
# CHECK-NEXT: <MCOperand Expr:(my_function)>

J 100
# CHECK: <MCInst #{{[0-9]+}} J
# CHECK-NEXT: <MCOperand Imm:100>

MOV x0, x1
# CHECK: <MCInst #{{[0-9]+}} MOV
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>

NOT x2, x3
# CHECK: <MCInst #{{[0-9]+}} NOT
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>

NEG x4, x5
# CHECK: <MCInst #{{[0-9]+}} NEG
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>

SNEZ x6, x7
# CHECK: <MCInst #{{[0-9]+}} SNEZ
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:9>

SLTZ x8, x9
# CHECK: <MCInst #{{[0-9]+}} SLTZ
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>

SGTZ x10, x11
# CHECK: <MCInst #{{[0-9]+}} SGTZ
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>

LLA x0, my_label
# CHECK: <MCInst #{{[0-9]+}} LLA
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Expr:(my_label)>

LI x1, my_label
# CHECK: <MCInst #{{[0-9]+}} LI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(my_label)>