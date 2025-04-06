# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-encoding < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

SB x0, 10(x1)
# CHECK: # encoding: [0x23,0x85,0x00,0x00]

SH x2, 15(x3)
# CHECK: # encoding: [0xa3,0x97,0x21,0x00]

SW x4, 20(x5)
# CHECK: # encoding: [0x23,0xaa,0x42,0x00]