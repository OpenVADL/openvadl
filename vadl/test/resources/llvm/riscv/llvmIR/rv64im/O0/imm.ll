; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O0 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define signext i32 @zero() nounwind {
  ; CHECK-LABEL: zero: # @zero
  ; CHECK: ADDI a0,zero,0
  ; CHECK-NEXT: RET
  ret i32 0
}

define signext i32 @pos_small() nounwind {
  ; CHECK-LABEL: pos_small: # @pos_small
  ; CHECK: ADDI a0,zero,2047
  ; CHECK-NEXT: RET
  ret i32 2047
}

define signext i32 @neg_small() nounwind {
  ; CHECK-LABEL: neg_small: # @neg_small
  ; CHECK: ADDI a0,zero,-2048
  ; CHECK-NEXT: RET
  ret i32 -2048
}

define signext i32 @pos_i32() nounwind {
  ; CHECK-LABEL: pos_i32: # @pos_i32
  ; CHECK: LUI a0,0x67783
  ; CHECK-NEXT: ADDI a0,a0,-1297
  ; CHECK-NEXT: RET
  ret i32 1735928559
}

define signext i32 @neg_i32() nounwind {
  ; CHECK-LABEL: neg_i32: # @neg_i32
  ; CHECK: LUI a0,0xdeadc
  ; CHECK-NEXT: ADDI a0,a0,-273
  ; CHECK-NEXT: RET
  ret i32 -559038737
}

define signext i32 @pos_i32_hi20_only() nounwind {
  ; CHECK-LABEL: pos_i32_hi20_only: # @pos_i32_hi20_only
  ; CHECK: LUI a0,0x10
  ; CHECK-NEXT: ADDI a0,a0,0
  ; CHECK-NEXT: RET
  ret i32 65536 ; 0x10000
}

define signext i32 @neg_i32_hi20_only() nounwind {
  ; CHECK-LABEL: neg_i32_hi20_only: # @neg_i32_hi20_only
  ; CHECK: LUI a0,0xffff0
  ; CHECK-NEXT: ADDI a0,a0,0
  ; CHECK-NEXT: RET
  ret i32 -65536 ; -0x10000
}

define i64 @imm_end_xori_1() nounwind {
  ; CHECK-LABEL: imm_end_xori_1: # @imm_end_xori_1
  ; CHECK: LUI a0,0xe0000
  ; CHECK-NEXT: ADDI a0,a0,0
  ; CHECK-NEXT: SLLI a0,a0,16
  ; CHECK-NEXT: ORI a0,a0,-512
  ; CHECK-NEXT: SLLI a0,a0,16
  ; CHECK-NEXT: ORI a0,a0,-1
  ; CHECK-NEXT: RET
  ret i64 -2305843009180139521 ; 0xE000_0000_01FF_FFFF
}

define void @imm_store_i8_neg1(ptr %p) nounwind {
  ; CHECK-LABEL: imm_store_i8_neg1: # @imm_store_i8_neg1
  ; CHECK: ADDI a1,zero,255
  ; CHECK-NEXT: SB a1,0(a0)
  ; CHECK-NEXT: RET
  store i8 -1, ptr %p
  ret void
}

define void @imm_store_i16_neg1(ptr %p) nounwind {
  ; CHECK-LABEL: imm_store_i16_neg1: # @imm_store_i16_neg1
  ; CHECK: LUI a1,0x10
  ; CHECK-NEXT: ADDI a1,a1,-1
  ; CHECK-NEXT: SH a1,0(a0)
  ; CHECK-NEXT: RET
  store i16 -1, ptr %p
  ret void
}

define void @imm_store_i32_neg1(ptr %p) nounwind {
  ; CHECK-LABEL: imm_store_i32_neg1: # @imm_store_i32_neg1
  ; CHECK: LUI a1,0x0
  ; CHECK-NEXT: a1,a1,0
  ; CHECK-NEXT: SLLI a1,a1,16
  ; CHECK-NEXT: ORI a1,a1,0
  ; CHECK-NEXT: SLLI a1,a1,16
  ; CHECK-NEXT: ORI a1,a1,-1
  ; CHECK-NEXT: SW a1,0(a0)
  ; CHECK-NEXT: RET
  store i32 -1, ptr %p
  ret void
}
