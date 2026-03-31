# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-encoding < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

JAL x16, 10
# CHECK: # encoding: [0x6f,0x08,0xa0,0x00]

JAL x16, -524288
# CHECK: # encoding: [0x6f,0x08,0x08,0x80]