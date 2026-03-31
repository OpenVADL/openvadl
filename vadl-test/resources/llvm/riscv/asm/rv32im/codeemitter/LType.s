# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-encoding < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

LB x0, 10(x1)
# CHECK: # encoding: [0x03,0x80,0xa0,0x00]

LBU x2, 15(x3)
# CHECK: # encoding: [0x03,0xc1,0xf1,0x00]

LH x4, 20(x5)
# CHECK: # encoding: [0x03,0x92,0x42,0x01]

LHU x6, 25(x7)
# CHECK: # encoding: [0x03,0xd3,0x93,0x01]

LW x8, 30(x9)
# CHECK: # encoding: [0x03,0xa4,0xe4,0x01]