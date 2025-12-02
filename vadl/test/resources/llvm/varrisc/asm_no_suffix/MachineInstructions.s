# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

ADD r1, r2
# CHECK: <MCInst #{{[0-9]+}} ADD_S
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>>

MUL r2, r3, r4
# CHECK: <MCInst #{{[0-9]+}} MUL
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>>

ADDI r1, 0xf
# CHECK: <MCInst #{{[0-9]+}} ADDI_S
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:15>>

ADDI r1, .label
# CHECK: <MCInst #{{[0-9]+}} ADDI_S
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(.label)>>

ADDI r1, r2, 5
# CHECK: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:5>>

ADDI r1, r2, 1024
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1024>>

ADDI r1, r2, .label
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Expr:(.label)>>

ASRI r1, 0xf
# CHECK: <MCInst #{{[0-9]+}} ASRI_S
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:15>>

ASRI r1, .label
# CHECK: <MCInst #{{[0-9]+}} ASRI_S
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(.label)>>

ASRI r1, r2, 5
# CHECK: <MCInst #{{[0-9]+}} ASRI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:5>>

ASRI r1, r2, .label
# CHECK: <MCInst #{{[0-9]+}} ASRI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Expr:(.label)>>

BEQ r1, r2, 0x1
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2>>

BEQ r1, r2, 1024
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1024>>

BEQ r1, r2, .label
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Expr:(.label)>>

LDB r1, 0x1 (r2)
# CHECK: <MCInst #{{[0-9]+}} LDB
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1>>

LDB r1, 1024 (r2)
# CHECK: <MCInst #{{[0-9]+}} LDB_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1024>>

LDB r1, .label (r2)
# CHECK: <MCInst #{{[0-9]+}} LDB_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Expr:(.label)>>

LDB r1, (r2)
# CHECK: <MCInst #{{[0-9]+}} LDB_S
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>>

SEQ r1, r2, r3
# CHECK: <MCInst #{{[0-9]+}} SEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>>

SGTI r1, r2, 1
# CHECK: <MCInst #{{[0-9]+}} SGTI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1>>

SGTI r1, r2, .label
# CHECK: <MCInst #{{[0-9]+}} SGTI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Expr:(.label)>>

SGTI r1, -1025
# CHECK: <MCInst #{{[0-9]+}} SGTI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:-1025>>

SGTI r1, .label
# CHECK: <MCInst #{{[0-9]+}} SGTI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(.label)>>