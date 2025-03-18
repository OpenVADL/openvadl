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
# CHECK-NEXT: <MCOperand Expr:(my_function)>>

TAIL my_function
# CHECK: <MCInst #{{[0-9]+}} TAIL
# CHECK-NEXT: <MCOperand Expr:(my_function)>>

J 100
# CHECK: <MCInst #{{[0-9]+}} J
# CHECK-NEXT: <MCOperand Imm:100>>

MOV x0, x1
# CHECK: <MCInst #{{[0-9]+}} MOV
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>>

NOT x2, x3
# CHECK: <MCInst #{{[0-9]+}} NOT
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>>

NEG x4, x5
# CHECK: <MCInst #{{[0-9]+}} NEG
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>>

SNEZ x6, x7
# CHECK: <MCInst #{{[0-9]+}} SNEZ
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:9>>

SLTZ x8, x9
# CHECK: <MCInst #{{[0-9]+}} SLTZ
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>>

SGTZ x10, x11
# CHECK: <MCInst #{{[0-9]+}} SGTZ
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>>

BEQZ x1, 1
# CHECK: <MCInst #{{[0-9]+}} BEQZ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:1>>

BNEZ x2, 2
# CHECK: <MCInst #{{[0-9]+}} BNEZ
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2>>

BLEZ x3, 3
# CHECK: <MCInst #{{[0-9]+}} BLEZ
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:3>>

BGEZ x4, 4
# CHECK: <MCInst #{{[0-9]+}} BGEZ
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Imm:4>>

BLTZ x5, 5
# CHECK: <MCInst #{{[0-9]+}} BLTZ
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Imm:5>>

BGTZ x6, 6
# CHECK: <MCInst #{{[0-9]+}} BGTZ
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:6>>

LLA x0, my_label
# CHECK: <MCInst #{{[0-9]+}} LLA
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Expr:(my_label)>>

LI x1, my_label
# CHECK: <MCInst #{{[0-9]+}} LI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(my_label)>>