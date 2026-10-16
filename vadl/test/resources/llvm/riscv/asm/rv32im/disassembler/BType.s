# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -disassemble -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

# BEQ x1, x2, .label (unresolved fixup -> offset 0)
0x63 0x80 0x20 0x00
# CHECK: <MCInst #{{[0-9]+}} BEQ{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:0>>

# BNE x3, x4, 4
0x63 0x92 0x41 0x00
# CHECK: <MCInst #{{[0-9]+}} BNE{{$}}
# CHECK-NEXT: <MCOperand Reg:5>
# CHECK-NEXT: <MCOperand Reg:6>
# CHECK-NEXT: <MCOperand Imm:4>>

# BGEU x7, x8, 2046
0x63 0xff 0x83 0x7e
# CHECK: <MCInst #{{[0-9]+}} BGEU{{$}}
# CHECK-NEXT: <MCOperand Reg:9>
# CHECK-NEXT: <MCOperand Reg:10>
# CHECK-NEXT: <MCOperand Imm:2046>>

# BLT x9, x10, -2048
0xe3 0xc0 0xa4 0x80
# CHECK: <MCInst #{{[0-9]+}} BLT{{$}}
# CHECK-NEXT: <MCOperand Reg:11>
# CHECK-NEXT: <MCOperand Reg:12>
# CHECK-NEXT: <MCOperand Imm:-2048>>

# BLTU x11, x12, .label (unresolved fixup -> offset 0)
0x63 0xe0 0xc5 0x00
# CHECK: <MCInst #{{[0-9]+}} BLTU{{$}}
# CHECK-NEXT: <MCOperand Reg:13>
# CHECK-NEXT: <MCOperand Reg:14>
# CHECK-NEXT: <MCOperand Imm:0>>

