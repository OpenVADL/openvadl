# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

ADD_S r0, r1
# CHECK: <MCInst #{{[0-9]+}} ADD_S
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>>

MUL r2, r3, r4
# CHECK: <MCInst #{{[0-9]+}} MUL
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>>

DIVI r5, r6, 0xff
# CHECK: <MCInst #{{[0-9]+}} DIVI
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Imm:255>>

REMI_L r7, r8, 0x1234
# CHECK: <MCInst #{{[0-9]+}} REMI_L
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Imm:4660>>

SUBI_S r9, 20
# CHECK: <MCInst #{{[0-9]+}} SUBI_S
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Imm:20>>

MOV r10 := r11
# CHECK: <MCInst #{{[0-9]+}} MOV
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>>

BRA_S 10
# CHECK: <MCInst #{{[0-9]+}} BRA_S
# CHECK-NEXT: <MCOperand Imm:20>>

JAL r12, r13
# CHECK: <MCInst #{{[0-9]+}} JAL
# CHECK-NEXT: <MCOperand Reg:14>
# CHECK-NEXT: <MCOperand Reg:15>>

BEQ r14, r15, 20
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:16>
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Imm:40>>

BEQ_L r14, r15, 20
# CHECK: <MCInst #{{[0-9]+}} BEQ
# CHECK-NEXT: <MCOperand Reg:16>
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Imm:20>>

LDB r16, 2(r17)
# CHECK: <MCInst #{{[0-9]+}} LDB
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:19>
# CHECK-NEXT: <MCOperand Imm:2>>

LDH_S r18, (r19)
# CHECK: <MCInst #{{[0-9]+}} LDH_S
# CHECK-NEXT: <MCOperand Reg:20>
# CHECK-NEXT: <MCOperand Reg:21>>

LDW_L r20, 0x12345(r21)
# CHECK: <MCInst #{{[0-9]+}} LDW_L
# CHECK-NEXT: <MCOperand Reg:22>
# CHECK-NEXT: <MCOperand Reg:23>
# CHECK-NEXT: <MCOperand Imm:74565>>

BAL r22, 0x12
# CHECK: <MCInst #{{[0-9]+}} BAL
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Imm:36>>

BAL_L r22, 0xffff
# CHECK: <MCInst #{{[0-9]+}} BAL_L
# CHECK-NEXT: <MCOperand Reg:24>
# CHECK-NEXT: <MCOperand Imm:65535>>

SEQ r23, r24, r25
# CHECK: <MCInst #{{[0-9]+}} SEQ
# CHECK-NEXT: <MCOperand Reg:25>
# CHECK-NEXT: <MCOperand Reg:26>
# CHECK-NEXT: <MCOperand Reg:27>>

SGTI r26, r27, 100
# CHECK: <MCInst #{{[0-9]+}} SGTI
# CHECK-NEXT: <MCOperand Reg:28>
# CHECK-NEXT: <MCOperand Reg:29>
# CHECK-NEXT: <MCOperand Imm:100>>

SGEI_L r28, 0x7fffffff
# CHECK: <MCInst #{{[0-9]+}} SGEI_L
# CHECK-NEXT: <MCOperand Reg:30>
# CHECK-NEXT: <MCOperand Reg:30>
# CHECK-NEXT: <MCOperand Imm:2147483647>>