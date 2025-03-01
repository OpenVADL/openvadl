# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

# Test cases for ADD
ADD x0, x1, x2
# CHECK: <MCInst #{{[0-9]+}} ADD
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>>

ADD zero, ra, sp
# CHECK: <MCInst #{{[0-9]+}} ADD
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>>

# Test cases for SUB
SUB x3, x4, x5
# CHECK: <MCInst #{{[0-9]+}} SUB
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>>

SUB gp, tp, t0
# CHECK: <MCInst #{{[0-9]+}} SUB
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>>

# Test cases for AND
AND x6, x7, x8
# CHECK: <MCInst #{{[0-9]+}} AND
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>>

AND t1, t2, fp
# CHECK: <MCInst #{{[0-9]+}} AND
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>>

# Test cases for OR
OR x9, x10, x11
# CHECK: <MCInst #{{[0-9]+}} OR
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>>

OR s1, a0, a1
# CHECK: <MCInst #{{[0-9]+}} OR
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>>

# Test cases for XOR
XOR x12, x13, x14
# CHECK: <MCInst #{{[0-9]+}} XOR
# CHECK-NEXT: <MCOperand Reg:14>
# CHECK-NEXT: <MCOperand Reg:15>
# CHECK-NEXT: <MCOperand Reg:16>>

XOR a2, a3, a4
# CHECK: <MCInst #{{[0-9]+}} XOR
# CHECK-NEXT: <MCOperand Reg:14>
# CHECK-NEXT: <MCOperand Reg:15>
# CHECK-NEXT: <MCOperand Reg:16>>

# Test cases for SLT
SLT x15, x16, x17
# CHECK: <MCInst #{{[0-9]+}} SLT
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:19>>

SLT a5, a6, a7
# CHECK: <MCInst #{{[0-9]+}} SLT
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:19>>

# Test cases for SLTU
SLTU x18, x19, x20
# CHECK: <MCInst #{{[0-9]+}} SLTU
# CHECK-NEXT: <MCOperand Reg:20>
# CHECK-NEXT: <MCOperand Reg:21>
# CHECK-NEXT: <MCOperand Reg:22>>

SLTU s2, s3, s4
# CHECK: <MCInst #{{[0-9]+}} SLTU
# CHECK-NEXT: <MCOperand Reg:20>
# CHECK-NEXT: <MCOperand Reg:21>
# CHECK-NEXT: <MCOperand Reg:22>>

# Test cases for SLL
SLL x21, x22, x23
# CHECK: <MCInst #{{[0-9]+}} SLL
# CHECK-NEXT: <MCOperand Reg:23>
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Reg:25>>

SLL s5, s6, s7
# CHECK: <MCInst #{{[0-9]+}} SLL
# CHECK-NEXT: <MCOperand Reg:23>
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Reg:25>>

# Test cases for SRL
SRL x24, x25, x26
# CHECK: <MCInst #{{[0-9]+}} SRL
# CHECK-NEXT: <MCOperand Reg:26>
# CHECK-NEXT: <MCOperand Reg:27>
# CHECK-NEXT: <MCOperand Reg:28>>

SRL s8, s9, s10
# CHECK: <MCInst #{{[0-9]+}} SRL
# CHECK-NEXT: <MCOperand Reg:26>
# CHECK-NEXT: <MCOperand Reg:27>
# CHECK-NEXT: <MCOperand Reg:28>>

# Test cases for SRA
SRA x27, x28, x29
# CHECK: <MCInst #{{[0-9]+}} SRA
# CHECK-NEXT: <MCOperand Reg:29>
# CHECK-NEXT: <MCOperand Reg:30>
# CHECK-NEXT: <MCOperand Reg:31>>

SRA s11, t3, t4
# CHECK: <MCInst #{{[0-9]+}} SRA
# CHECK-NEXT: <MCOperand Reg:29>
# CHECK-NEXT: <MCOperand Reg:30>
# CHECK-NEXT: <MCOperand Reg:31>>

# Test cases for MUL
MUL x30, x31, x8
# CHECK: <MCInst #{{[0-9]+}} MUL
# CHECK-NEXT: <MCOperand Reg:32>
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Reg:10>>

MUL t5, t6, s0
# CHECK: <MCInst #{{[0-9]+}} MUL
# CHECK-NEXT: <MCOperand Reg:32>
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Reg:10>>

# Test cases for MULH
MULH x4, x5, x6
# CHECK: <MCInst #{{[0-9]+}} MULH
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Reg:8>>

# Test cases for MULHSU
MULHSU x7, x8, x9
# CHECK: <MCInst #{{[0-9]+}} MULHSU
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>>

# Test cases for MULHU
MULHU x10, x11, x12
# CHECK: <MCInst #{{[0-9]+}} MULHU
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>
# CHECK-NEXT: <MCOperand Reg:14>>

# Test cases for DIV
DIV x13, x14, x15
# CHECK: <MCInst #{{[0-9]+}} DIV
# CHECK-NEXT: <MCOperand Reg:15>
# CHECK-NEXT: <MCOperand Reg:16>
# CHECK-NEXT: <MCOperand Reg:17>>

# Test cases for DIVU
DIVU x16, x17, x18
# CHECK: <MCInst #{{[0-9]+}} DIVU
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:19>
# CHECK-NEXT: <MCOperand Reg:20>>

# Test cases for REM
REM x19, x20, x21
# CHECK: <MCInst #{{[0-9]+}} REM
# CHECK-NEXT: <MCOperand Reg:21>
# CHECK-NEXT: <MCOperand Reg:22>
# CHECK-NEXT: <MCOperand Reg:23>>

# Test cases for REMU
REMU x22, x23, x24
# CHECK: <MCInst #{{[0-9]+}} REMU
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Reg:25>
# CHECK-NEXT: <MCOperand Reg:26>>