# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT


bal r1, 32767
# CHECK: <MCInst #{{[0-9]+}} BAL
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:65534>>

bal r1, -32768
# CHECK: <MCInst #{{[0-9]+}} BAL
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:-65536>>

bal r1, 0x7fff
# CHECK: <MCInst #{{[0-9]+}} BAL
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:65534>>

bal r1, 0x8000
# CHECK: <MCInst #{{[0-9]+}} BAL_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:32768>

bal r1, 0x10000
# CHECK: <MCInst #{{[0-9]+}} BAL_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:65536>>

bal r1, 65536
# CHECK: <MCInst #{{[0-9]+}} BAL_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:65536>>



addi r1, r2, 1023
# CHECK: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1023>>

addi r1, r2, -1024
# CHECK: <MCInst #{{[0-9]+}} ADDI
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:64512>>

addi r1, r2, 1024
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1024>>

addi r1, r2, -1025
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:-1025>>

addi r1, r2, 0x7ff
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2047>>

addi r1, r2, 2047
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2047>>

addi r1, r2, 2048
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2048>>

addi r1, r2, 2049
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2049>>

addi r1, r2, -2049
# CHECK: <MCInst #{{[0-9]+}} ADDI_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:-2049>>



BEQ r1, r2, 1023
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:2046>>

BEQ r1, r2, -1024
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:-2048>>

BEQ r1, r2, 1024
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1024>>

BEQ r1, r2, -1025
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:-1025>>



LDB r1, 1023 (r2)
# CHECK: <MCInst #{{[0-9]+}} LDB
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1023>>

LDB r1, 1024 (r2)
# CHECK: <MCInst #{{[0-9]+}} LDB_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:1024>>

# operand of LBD is unsigned, therefore -1 is 4294967295
# and LDB_L is selected
LDB r1, -1 (r2)
# CHECK: <MCInst #{{[0-9]+}} LDB_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:4294967295>>


BRA 511
# CHECK: <MCInst #{{[0-9]+}} BRA_S
# CHECK-NEXT: <MCOperand Imm:1022>>

BRA 512
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:512>>

BRA 1023
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:1023>>

BRA -1024
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:-1024>>

BRA 1024
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:1024>>

BRA -1025
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:-1025>>



BEQZ r1, 1023
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:1023>>

BEQZ r1, -1024
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:-1024>>

BEQZ r1, 1024
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:1024>>

BEQZ r1, -1025
# CHECK: <MCInst #{{[0-9]+}} BEQ_L
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Imm:-1025>>