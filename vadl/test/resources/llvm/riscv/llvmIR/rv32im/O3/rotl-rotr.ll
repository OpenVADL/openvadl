; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

; These IR sequences are idioms for rotates. If rotate instructions are
; supported, they will be turned into ISD::ROTL or ISD::ROTR.

define i32 @rotl_32(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotl_32:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,32
; CHECK-NEXT: SUB a2,a2,a1
; CHECK-NEXT: SLL a1,a0,a1
; CHECK-NEXT: SRL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: JALR zero,0(ra)
  %z = sub i32 32, %y
  %b = shl i32 %x, %y
  %c = lshr i32 %x, %z
  %d = or i32 %b, %c
  ret i32 %d
}

define i32 @rotr_32(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotr_32:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,32
; CHECK-NEXT: SUB a2,a2,a1
; CHECK-NEXT: SRL a1,a0,a1
; CHECK-NEXT: SLL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: JALR zero,0(ra)
  %z = sub i32 32, %y
  %b = lshr i32 %x, %y
  %c = shl i32 %x, %z
  %d = or i32 %b, %c
  ret i32 %d
}

define i64 @rotl_64(i64 %x, i64 %y) nounwind {
; CHECK-LABEL: rotl_64:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-32
; CHECK-NEXT: SW ra,28(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s1,24(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s2,20(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s3,16(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s4,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s5,8(sp) # 4-byte Folded Spill
; CHECK-NEXT: ADDI s1,a2,0
; CHECK-NEXT: ADDI s2,a1,0
; CHECK-NEXT: ADDI s3,a0,0
; CHECK-NEXT: LUI ra,%hi(__ashldi3)
; CHECK-NEXT: JALR ra,%lo(__ashldi3)(ra)
; CHECK-NEXT: ADDI s4,a0,0
; CHECK-NEXT: ADDI s5,a1,0
; CHECK-NEXT: ADDI a0,zero,64
; CHECK-NEXT: SUB a2,a0,s1
; CHECK-NEXT: ADDI a0,s3,0
; CHECK-NEXT: ADDI a1,s2,0
; CHECK-NEXT: LUI ra,%hi(__lshrdi3)
; CHECK-NEXT: JALR ra,%lo(__lshrdi3)(ra)
; CHECK-NEXT: OR a0,s4,a0
; CHECK-NEXT: OR a1,s5,a1
; CHECK-NEXT: LW s5,8(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s4,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s3,16(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s2,20(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s1,24(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW ra,28(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,32
; CHECK-NEXT: JALR zero,0(ra)
  %z = sub i64 64, %y
  %b = shl i64 %x, %y
  %c = lshr i64 %x, %z
  %d = or i64 %b, %c
  ret i64 %d
}

define i64 @rotr_64(i64 %x, i64 %y) nounwind {
; CHECK-LABEL: rotr_64:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-32
; CHECK-NEXT: SW ra,28(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s1,24(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s2,20(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s3,16(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s4,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s5,8(sp) # 4-byte Folded Spill
; CHECK-NEXT: ADDI s1,a2,0
; CHECK-NEXT: ADDI s2,a1,0
; CHECK-NEXT: ADDI s3,a0,0
; CHECK-NEXT: LUI ra,%hi(__lshrdi3)
; CHECK-NEXT: JALR ra,%lo(__lshrdi3)(ra)
; CHECK-NEXT: ADDI s4,a0,0
; CHECK-NEXT: ADDI s5,a1,0
; CHECK-NEXT: ADDI a0,zero,64
; CHECK-NEXT: SUB a2,a0,s1
; CHECK-NEXT: ADDI a0,s3,0
; CHECK-NEXT: ADDI a1,s2,0
; CHECK-NEXT: LUI ra,%hi(__ashldi3)
; CHECK-NEXT: JALR ra,%lo(__ashldi3)(ra)
; CHECK-NEXT: OR a0,s4,a0
; CHECK-NEXT: OR a1,s5,a1
; CHECK-NEXT: LW s5,8(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s4,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s3,16(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s2,20(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s1,24(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW ra,28(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,32
; CHECK-NEXT: JALR zero,0(ra)
  %z = sub i64 64, %y
  %b = lshr i64 %x, %y
  %c = shl i64 %x, %z
  %d = or i64 %b, %c
  ret i64 %d
}

define i32 @rotl_32_mask(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotl_32_mask:
; CHECK: # %bb.0:
; CHECK-NEXT: SUB a2,zero,a1
; CHECK-NEXT: SLL a1,a0,a1
; CHECK-NEXT: ANDI a2,a2,31
; CHECK-NEXT: SRL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: JALR zero,0(ra)
  %z = sub i32 0, %y
  %and = and i32 %z, 31
  %b = shl i32 %x, %y
  %c = lshr i32 %x, %and
  %d = or i32 %b, %c
  ret i32 %d
}

define i32 @rotl_32_mask_and_63_and_31(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotl_32_mask_and_63_and_31:
; CHECK: # %bb.0:
; CHECK-NEXT: SUB a2,zero,a1
; CHECK-NEXT: ANDI a1,a1,63
; CHECK-NEXT: SLL a1,a0,a1
; CHECK-NEXT: ANDI a2,a2,31
; CHECK-NEXT: SRL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: JALR zero,0(ra)
  %a = and i32 %y, 63
  %b = shl i32 %x, %a
  %c = sub i32 0, %y
  %d = and i32 %c, 31
  %e = lshr i32 %x, %d
  %f = or i32 %b, %e
  ret i32 %f
}

define i32 @rotl_32_mask_or_64_or_32(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotl_32_mask_or_64_or_32:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI a0,zero,0
; CHECK-NEXT: JALR zero,0(ra)
  %a = or i32 %y, 64
  %b = shl i32 %x, %a
  %c = sub i32 0, %y
  %d = or i32 %c, 32
  %e = lshr i32 %x, %d
  %f = or i32 %b, %e
  ret i32 %f
}

define i32 @rotr_32_mask(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotr_32_mask:
; CHECK: # %bb.0:
; CHECK-NEXT: SUB a2,zero,a1
; CHECK-NEXT: SRL a1,a0,a1
; CHECK-NEXT: ANDI a2,a2,31
; CHECK-NEXT: SLL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: JALR zero,0(ra)
  %z = sub i32 0, %y
  %and = and i32 %z, 31
  %b = lshr i32 %x, %y
  %c = shl i32 %x, %and
  %d = or i32 %b, %c
  ret i32 %d
}

define i32 @rotr_32_mask_and_63_and_31(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotr_32_mask_and_63_and_31:
; CHECK: # %bb.0:
; CHECK-NEXT: SUB a2,zero,a1
; CHECK-NEXT: ANDI a1,a1,63
; CHECK-NEXT: SRL a1,a0,a1
; CHECK-NEXT: ANDI a2,a2,31
; CHECK-NEXT: SLL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: JALR zero,0(ra)
  %a = and i32 %y, 63
  %b = lshr i32 %x, %a
  %c = sub i32 0, %y
  %d = and i32 %c, 31
  %e = shl i32 %x, %d
  %f = or i32 %b, %e
  ret i32 %f
}

define i32 @rotr_32_mask_or_64_or_32(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotr_32_mask_or_64_or_32:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI a0,zero,0
; CHECK-NEXT: JALR zero,0(ra)
  %a = or i32 %y, 64
  %b = lshr i32 %x, %a
  %c = sub i32 0, %y
  %d = or i32 %c, 32
  %e = shl i32 %x, %d
  %f = or i32 %b, %e
  ret i32 %f
}

define i64 @rotl_64_mask(i64 %x, i64 %y) nounwind {
; CHECK-LABEL: rotl_64_mask:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-32
; CHECK-NEXT: SW ra,28(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s1,24(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s2,20(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s3,16(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s4,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW s5,8(sp) # 4-byte Folded Spill
; CHECK-NEXT: ADDI s1,a2,0
; CHECK-NEXT: ADDI s2,a1,0
; CHECK-NEXT: ADDI s3,a0,0
; CHECK-NEXT: LUI ra,%hi(__ashldi3)
; CHECK-NEXT: JALR ra,%lo(__ashldi3)(ra)
; CHECK-NEXT: ADDI s4,a0,0
; CHECK-NEXT: ADDI s5,a1,0
; CHECK-NEXT: SUB a0,zero,s1
; CHECK-NEXT: ANDI a2,a0,63
; CHECK-NEXT: ADDI a0,s3,0
; CHECK-NEXT: ADDI a1,s2,0
; CHECK-NEXT: LUI ra,%hi(__lshrdi3)
; CHECK-NEXT: JALR ra,%lo(__lshrdi3)(ra)
; CHECK-NEXT: OR a0,s4,a0
; CHECK-NEXT: OR a1,s5,a1
; CHECK-NEXT: LW s5,8(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s4,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s3,16(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s2,20(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW s1,24(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW ra,28(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,32
; CHECK-NEXT: JALR zero,0(ra)
  %z = sub i64 0, %y
  %and = and i64 %z, 63
  %b = shl i64 %x, %y
  %c = lshr i64 %x, %and
  %d = or i64 %b, %c
  ret i64 %d
}