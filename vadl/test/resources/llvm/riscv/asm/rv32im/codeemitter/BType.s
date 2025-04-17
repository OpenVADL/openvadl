# RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-encoding < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

BEQ x1, x2, .label
# CHECK: # encoding: [0x63,0x80,0x20,0x00]
# CHECK-NEXT: #   fixup A - offset: 0, value: .label, kind: fixup_imm_RV3264I_Btype_ABSOLUTE_imm

BNE x3, x4, 4
# CHECK: # encoding: [0x63,0x92,0x41,0x00]

BGE x5, x6, 3
# CHECK: # encoding: [0x63,0xd1,0x62,0x00]

BGEU x7, x8, 2046
# CHECK: # encoding: [0x63,0xff,0x83,0x7e]

BLT x9, x10, -2048
# CHECK: # encoding: [0xe3,0xc0,0xa4,0x80]

BLTU x11, x12, .label
# CHECK: # encoding: [0x63,0xe0,0xc5,0x00]
# CHECK-NEXT: #   fixup A - offset: 0, value: .label, kind: fixup_imm_RV3264I_Btype_ABSOLUTE_imm