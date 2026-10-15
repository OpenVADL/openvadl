# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -disassemble -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

# ADDI x0, x1, 0xFF
0x13 0x80 0xf0 0x0f
# CHECK: <MCInst #{{[0-9]+}} ADDI{{$}}
# CHECK-NEXT: <MCOperand Reg:2>
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:255>>

# ANDI x2, x3, 0xFF
0x13 0xf1 0xf1 0x0f
# CHECK: <MCInst #{{[0-9]+}} ANDI{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Imm:255>>

# ORI x4, x5, 20
0x13 0xe2 0x42 0x01
# CHECK: <MCInst #{{[0-9]+}} ORI{{$}}
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Reg:7>
# CHECK-NEXT: <MCOperand Imm:20>>

# XORI x6, x7, 25
0x13 0xc3 0x93 0x01
# CHECK: <MCInst #{{[0-9]+}} XORI{{$}}
# CHECK-NEXT: <MCOperand Reg:8>
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Imm:25>>

# SLTI x8, x9, 0xFF
0x13 0xa4 0xf4 0x0f
# CHECK: <MCInst #{{[0-9]+}} SLTI{{$}}
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Imm:255>>

# SLTIU x10, x11, 35
0x13 0xb5 0x35 0x02
# CHECK: <MCInst #{{[0-9]+}} SLTIU{{$}}
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Reg:13>
# CHECK-NEXT: <MCOperand Imm:35>>

# JALR x14, x15, 45
0x67 0x87 0xd7 0x02
# CHECK: <MCInst #{{[0-9]+}} JALR{{$}}
# CHECK-NEXT: <MCOperand Reg:16>
# CHECK-NEXT: <MCOperand Reg:17>
# CHECK-NEXT: <MCOperand Imm:45>>

