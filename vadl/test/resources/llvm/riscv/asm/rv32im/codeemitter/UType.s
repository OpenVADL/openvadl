# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-encoding < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

LUI x1, 0x10
# CHECK: # encoding: [0xb7,0x00,0x01,0x00]

LUI x1, 0xF
# CHECK: # encoding: [0xb7,0xf0,0x00,0x00]

AUIPC x2, 10
# CHECK: # encoding: [0x17,0xa1,0x00,0x00]