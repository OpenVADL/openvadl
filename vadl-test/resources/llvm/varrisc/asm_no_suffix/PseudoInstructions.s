# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

BRA 10
# CHECK: <MCInst #{{[0-9]+}} BRA
# CHECK-NEXT: <MCOperand Imm:10>>

BRA 0x12345678
# CHECK: <MCInst #{{[0-9]+}} BRA_L
# CHECK-NEXT: <MCOperand Imm:305419896>>

BRA .label
# CHECK: <MCInst #{{[0-9]+}} BRA_L
# CHECK-NEXT: <MCOperand Expr:(.label)>>

BEQZ r1, 0x12
# CHECK: <MCInst #{{[0-9]+}} BEQZ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:18>>

BEQZ r1, 1024
# CHECK: <MCInst #{{[0-9]+}} BEQZ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:1024>>

BEQZ r1, .label
# CHECK: <MCInst #{{[0-9]+}} BEQZ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(.label)>>

JMP r10
# CHECK: <MCInst #{{[0-9]+}} JMP
# CHECK-NEXT: <MCOperand Reg:12>>

RET
# CHECK: <MCInst #{{[0-9]+}} RET>

CALL mylabel
# CHECK: <MCInst #{{[0-9]+}} CALL
# CHECK-NEXT: <MCOperand Expr:(mylabel)>>

LA r1, mylabel
# CHECK: <MCInst #{{[0-9]+}} LA
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(mylabel)>>