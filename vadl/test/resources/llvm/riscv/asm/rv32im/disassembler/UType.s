# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -disassemble -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

# LUI x1, 0x10
0xb7 0x00 0x01 0x00
# CHECK: <MCInst #{{[0-9]+}} LUI{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:65536>>

# LUI x1, 0xF
0xb7 0xf0 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} LUI{{$}}
# CHECK-NEXT: <MCOperand Reg:3>
# CHECK-NEXT: <MCOperand Imm:61440>>

# AUIPC x2, 10
0x17 0xa1 0x00 0x00
# CHECK: <MCInst #{{[0-9]+}} AUIPC{{$}}
# CHECK-NEXT: <MCOperand Reg:4>
# CHECK-NEXT: <MCOperand Imm:40960>>

