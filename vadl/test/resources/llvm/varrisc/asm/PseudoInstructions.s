# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

BRA 0x12
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:18>>

BRA_L 0x12345678
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:305419896>>

BEQZ r1, 0x12
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:18>>

BEQZ_L r1, 0x12345678
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:305419896>>

JMP r10
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:2>>

RET
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Reg:2>>

CALL mylabel
# CHECK: <MCInst #{{[0-9]+}} BAL_L
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Expr:(mylabel)>>

LA r1, mylabel
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Expr:(mylabel)>>