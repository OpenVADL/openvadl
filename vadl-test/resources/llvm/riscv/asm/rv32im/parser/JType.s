# error cases have to be listed first because stderr is prepended to stdout

# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

# immediate value overflow
JAL x16, 1048576
# CHECK: error: Invalid immediate operand for immS. The predicate does not hold.

# immediate value underflow
JAL x16, -1048578
# CHECK: error: Invalid immediate operand for immS. The predicate does not hold.

JAL x16, 10
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Imm:10>>

# maximum valid immediate value
JAL x16, 524286
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Imm:524286>>

# minimum valid immediate value
JAL x16, -524288
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Imm:-524288>>