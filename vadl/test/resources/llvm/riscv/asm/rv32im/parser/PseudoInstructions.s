# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT


RET
# CHECK: <MCInst #{{[0-9]+}} JALR
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:0>>

NOP
# CHECK: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:0>>

ECALL
# CHECK: <MCInst #{{[0-9]+}} ECALL>

EBREAK
# CHECK: <MCInst #{{[0-9]+}} EBREAK>


CALL my_function
# CHECK:      .Lhi_label0:
# CHECK-NEXT: <MCInst #{{[0-9]+}} AUIPC
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(%pcrel_hi(my_function))>>
# CHECK-NEXT: <MCInst #{{[0-9]+}} JALR
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(%pcrel_lo(.Lhi_label0))>>

TAIL my_function
# CHECK:      .Lhi_label1:
# CHECK-NEXT: <MCInst #{{[0-9]+}} AUIPC
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Expr:(%pcrel_hi(my_function))>>
# CHECK-NEXT: <MCInst #{{[0-9]+}} JALR
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Expr:(%pcrel_lo(.Lhi_label1))>>

J 100
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:100>>

JR a5
# CHECK: <MCInst #{{[0-9]+}} JALR
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Imm:0>>

MV x0, x1
# CHECK: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:0>>

NOT x2, x3
# CHECK: <MCInst #{{[0-9]+}} XORI
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:4095>>

NEG x4, x5
# CHECK: <MCInst #{{[0-9]+}} SUB
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:7>>

SNEZ x6, x7
# CHECK: <MCInst #{{[0-9]+}} SLTU
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:9>>

SLTZ x8, x9
# CHECK: <MCInst #{{[0-9]+}} SLT
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:2>>

SGTZ x10, x11
# CHECK: <MCInst #{{[0-9]+}} SLT
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:13>>

BEQZ x1, 1
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:1>>

BNEZ x2, 2
# CHECK: <MCInst #{{[0-9]+}} BNE
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:2>>

BLEZ x3, 3
# CHECK: <MCInst #{{[0-9]+}} BGE
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:3>>

BGEZ x4, 4
# CHECK: <MCInst #{{[0-9]+}} BGE
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:4>>

BLTZ x5, 5
# CHECK: <MCInst #{{[0-9]+}} BLT
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:5>>

BGTZ x6, 6
# CHECK: <MCInst #{{[0-9]+}} BLT
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:6>>

LLA x0, my_label
# CHECK:      .Lhi_label2:
# CHECK-NEXT: <MCInst #{{[0-9]+}} AUIPC
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Expr:(%pcrel_hi(my_label))>>
# CHECK-NEXT: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Expr:(%pcrel_lo(.Lhi_label2))>>

LI x1, my_label
# CHECK: <MCInst #{{[0-9]+}} LUI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(%hi(my_label))>>
# CHECK-NEXT: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(%lo(my_label))>>

LA x2, my_label
LI x1, my_label
# CHECK: <MCInst #{{[0-9]+}} LUI
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Expr:(%hi(my_label))>>
# CHECK-NEXT: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Expr:(%lo(my_label))>>

LGA x3, my_label
# CHECK:      .Lhi_label3:
# CHECK-NEXT: <MCInst #{{[0-9]+}} AUIPC
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Expr:(%got_pcrel_hi(my_label))>>
# CHECK-NEXT: <MCInst #{{[0-9]+}} LW
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Expr:(%pcrel_lo(.Lhi_label3))>>