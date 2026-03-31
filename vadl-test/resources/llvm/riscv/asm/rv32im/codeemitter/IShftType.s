# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-encoding < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

SLLI x0, x1, 2
# CHECK: # encoding: [0x13,0x90,0x20,0x00]

SRLI x2, x3, 3
# CHECK: # encoding: [0x13,0xd1,0x31,0x00]

SRAI x4, x5, 4
# CHECK: # encoding: [0x13,0xd2,0x42,0x40]