# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

JAL x16, 10
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Imm:20>>

# maximum valid immediate value
JAL x16, 524287
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Imm:1048574>>

# immediate value overflow
JAL x16, 524288
# CHECK: error: Invalid immediate operand for JAL.imm. Value {{[0-9]+}} is out of the valid range {{.*}}

# minimum valid immediate value
JAL x16, -524288
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Imm:-1048576>>

# immediate value underflow
JAL x16, -524289
# CHECK: error: Invalid immediate operand for JAL.imm. Value {{[0-9]+}} is out of the valid range {{.*}}