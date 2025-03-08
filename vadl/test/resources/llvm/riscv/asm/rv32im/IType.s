# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

ADDI x0, x1, %lo(0xFFFF)
# CHECK: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Expr:(4095)>>

ANDI x2, x3, 0xFF
# CHECK: <MCInst #{{[0-9]+}} ANDI
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:255>>

ORI x4, x5, 20
# CHECK: <MCInst #{{[0-9]+}} ORI
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Imm:20>>

XORI x6, x7, 25
# CHECK: <MCInst #{{[0-9]+}} XORI
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Imm:25>>

SLTI x8, x9, 0xFFF
# CHECK: <MCInst #{{[0-9]+}} SLTI
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Imm:4095>>

SLTIU x10, x11, 35
# CHECK: <MCInst #{{[0-9]+}} SLTIU
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>
# CHECK-NEXT: <MCOperand Imm:35>>

JALR x14, x15, 45
# CHECK: <MCInst #{{[0-9]+}} JALR
# CHECK-NEXT: <MCOperand Reg:16>
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Imm:45>>