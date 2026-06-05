# error cases have to be listed first because stderr is prepended to stdout

# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

addi r1, r2, 0x7ffffffff
# CHECK: error: Invalid immediate operand for imm. Value is larger than 11 bits.

addi r1, r2, 2048
# CHECK: error: Invalid immediate operand for imm. Value is larger than 11 bits.

addi r1, r2, 0x800
# CHECK: error: Invalid immediate operand for imm. Value is larger than 11 bits.

LBU r1, 0x7ff (r2)
# CHECK: error: Invalid immediate operand for imm. The predicate does not hold.

LBU r1, 0xfff (r2)
# CHECK: error: Invalid immediate operand for imm. Value is larger than 11 bits.

bal r1, 0x10000
# CHECK: error: Invalid immediate operand for imm. Value is larger than 16 bits.

BEQZ r1, 0xffffffffffff
# CHECK: error: Invalid immediate operand for offset. Value is out of range (-1024,1023).

BEQZ r1, 1024
# CHECK: error: Invalid immediate operand for offset. Value is out of range (-1024,1023).

BEQZ r1, -1025
# CHECK: error: Invalid immediate operand for offset. Value is out of range (-1024,1023).

bal_l r1, 0x1ffffffff
# CHECK: error: Invalid immediate operand for imm. Value is larger than 32 bits.