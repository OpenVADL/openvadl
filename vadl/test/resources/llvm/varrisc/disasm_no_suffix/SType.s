# RUN: /src/llvm-final/build/bin/llvm-mc -arch=varrisc -disassemble -show-inst < $INPUT 2>&1 | /src/llvm-final/build/bin/FileCheck $INPUT

# Encodings derived from sys/v-risc/VarRisc.vadl, imported by ABI_no_suffix.vadl.
# TYPE_S: rs1imm[15:11] | rd[10:6] | opcode[5:0], little-endian bytes.
# LLVM register IDs are R(n) + 2; two-address operations repeat rd as an input.

# ADD r0, r31 (ADD_S, opcode=0)
0x00 0xf8
# CHECK: <MCInst #{{[0-9]+}} ADD_S{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:33>>

# SUB r7, r9 (SUB_S, opcode=1)
0xc1 0x49
# CHECK: <MCInst #{{[0-9]+}} SUB_S{{$}}
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:11>>

# ADDI r1, 15 (ADDI_S, opcode=CodeOff+0=11)
0x4b 0x78
# CHECK: <MCInst #{{[0-9]+}} ADDI_S{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:15>>

# ADDI r2, 16 (ADDI_S; imm is Bits<5>, so `imm as SIntR` zero-extends)
0x8b 0x80
# CHECK: <MCInst #{{[0-9]+}} ADDI_S{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:16>>

# ADDI r2, 31 (ADDI_S, unsigned 5-bit maximum)
0x8b 0xf8
# CHECK: <MCInst #{{[0-9]+}} ADDI_S{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:31>>

# LSLI r31, 31 (LSLI_S, opcode=CodeOff+10=21, unsigned shift amount)
0xd5 0xff
# CHECK: <MCInst #{{[0-9]+}} LSLI_S{{$}}
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Imm:31>>

# MOV r10 := r11 (opcode=22)
0x96 0x5a
# CHECK: <MCInst #{{[0-9]+}} MOV{{$}}
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>>

# JAL r12, r13 (opcode=24)
0x18 0x6b
# CHECK: <MCInst #{{[0-9]+}} JAL{{$}}
# CHECK-NEXT: <MCOperand Reg:14>
# CHECK-NEXT: <MCOperand Reg:15>>

# LDB r16, (r17) (LDB_S, opcode=MemSOff+0=26)
0x1a 0x8c
# CHECK: <MCInst #{{[0-9]+}} LDB_S{{$}}
# CHECK-NEXT: <MCOperand Reg:18>
# CHECK-NEXT: <MCOperand Reg:19>>

# LDH r18, (r19) (LDH_S, opcode=MemSOff+1=27)
0x9b 0x9c
# CHECK: <MCInst #{{[0-9]+}} LDH_S{{$}}
# CHECK-NEXT: <MCOperand Reg:20>
# CHECK-NEXT: <MCOperand Reg:21>>

# STW r30, (r31) (STW_S, opcode=MemSOff+5=31; base precedes stored value)
0x9f 0xff
# CHECK: <MCInst #{{[0-9]+}} STW_S{{$}}
# CHECK-NEXT: <MCOperand Reg:33>
# CHECK-NEXT: <MCOperand Reg:32>>
