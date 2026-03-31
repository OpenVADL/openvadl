; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O3 --relocation-model=pic -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

; These IR sequences are idioms for rotates. If rotate instructions are
; supported, they will be turned into ISD::ROTL or ISD::ROTR.

define i32 @rotl_32(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotl_32:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,32
; CHECK-NEXT: SUB a2,a2,a1
; CHECK-LABEL: .Ltmp0:
; CHECK-NEXT: AUIPC a3,%pcrel_hi(.LCPI0_0)
; CHECK-NEXT: ADDI a3,a3,%pcrel_lo(.Ltmp0)
; CHECK-NEXT: LD a3,0(a3)
; CHECK-NEXT: AND a2,a2,a3
; CHECK-NEXT: AND a4,a0,a3
; CHECK-NEXT: SRL a2,a4,a2
; CHECK-NEXT: AND a1,a1,a3
; CHECK-NEXT: SLL a0,a0,a1
; CHECK-NEXT: OR a0,a0,a2
; CHECK-NEXT: RET
  %z = sub i32 32, %y
  %b = shl i32 %x, %y
  %c = lshr i32 %x, %z
  %d = or i32 %b, %c
  ret i32 %d
}

define i32 @rotr_32(i32 %x, i32 %y) nounwind {
; CHECK-LABEL: rotr_32:
; CHECK: # %bb.0:
; CHECK-LABEL: .Ltmp1:
; CHECK-NEXT: AUIPC a2,%pcrel_hi(.LCPI1_0)
; CHECK-NEXT: ADDI a2,a2,%pcrel_lo(.Ltmp1)
; CHECK-NEXT: LD a2,0(a2)
; CHECK-NEXT: AND a3,a1,a2
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: SRL a3,a4,a3
; CHECK-NEXT: ADDI a4,zero,32
; CHECK-NEXT: SUB a1,a4,a1
; CHECK-NEXT: AND a1,a1,a2
; CHECK-NEXT: SLL a0,a0,a1
; CHECK-NEXT: OR a0,a3,a0
; CHECK-NEXT: RET
  %z = sub i32 32, %y
  %b = lshr i32 %x, %y
  %c = shl i32 %x, %z
  %d = or i32 %b, %c
  ret i32 %d
}

define i64 @rotl_64(i64 %x, i64 %y) nounwind {
; CHECK-LABEL: rotl_64:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,64
; CHECK-NEXT: SUB a2,a2,a1
; CHECK-NEXT: SLL a1,a0,a1
; CHECK-NEXT: SRL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: RET
  %z = sub i64 64, %y
  %b = shl i64 %x, %y
  %c = lshr i64 %x, %z
  %d = or i64 %b, %c
  ret i64 %d
}

define i64 @rotr_64(i64 %x, i64 %y) nounwind {
; CHECK-LABEL: rotr_64:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,64
; CHECK-NEXT: SUB a2,a2,a1
; CHECK-NEXT: SRL a1,a0,a1
; CHECK-NEXT: SLL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: RET
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
; CHECK-NEXT: ANDI a2,a2,31
; CHECK-LABEL: .Ltmp2:
; CHECK-NEXT: AUIPC a3,%pcrel_hi(.LCPI4_0)
; CHECK-NEXT: ADDI a3,a3,%pcrel_lo(.Ltmp2)
; CHECK-NEXT: LD a3,0(a3)
; CHECK-NEXT: AND a4,a0,a3
; CHECK-NEXT: SRL a2,a4,a2
; CHECK-NEXT: AND a1,a1,a3
; CHECK-NEXT: SLL a0,a0,a1
; CHECK-NEXT: OR a0,a0,a2
; CHECK-NEXT: RET
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
; CHECK-LABEL: .Ltmp3:
; CHECK-NEXT: AUIPC a2,%pcrel_hi(.LCPI5_0)
; CHECK-NEXT: ADDI a2,a2,%pcrel_lo(.Ltmp3)
; CHECK-NEXT: LD a2,0(a2)
; CHECK-NEXT: AND a2,a0,a2
; CHECK-NEXT: SUB a3,zero,a1
; CHECK-NEXT: ANDI a3,a3,31
; CHECK-NEXT: SRL a2,a2,a3
; CHECK-NEXT: ANDI a1,a1,63
; CHECK-NEXT: SLL a0,a0,a1
; CHECK-NEXT: OR a0,a0,a2
; CHECK-NEXT: RET
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
; CHECK-NEXT: RET
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
; CHECK-LABEL: .Ltmp4:
; CHECK-NEXT: AUIPC a2,%pcrel_hi(.LCPI7_0)
; CHECK-NEXT: ADDI a2,a2,%pcrel_lo(.Ltmp4)
; CHECK-NEXT: LD a2,0(a2)
; CHECK-NEXT: AND a3,a1,a2
; CHECK-NEXT: AND a2,a0,a2
; CHECK-NEXT: SRL a2,a2,a3
; CHECK-NEXT: SUB a1,zero,a1
; CHECK-NEXT: ANDI a1,a1,31
; CHECK-NEXT: SLL a0,a0,a1
; CHECK-NEXT: OR a0,a2,a0
; CHECK-NEXT: RET
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
; CHECK-NEXT: ANDI a2,a2,31
; CHECK-NEXT: SLL a2,a0,a2
; CHECK-LABEL: .Ltmp5:
; CHECK-NEXT: AUIPC a3,%pcrel_hi(.LCPI8_0)
; CHECK-NEXT: ADDI a3,a3,%pcrel_lo(.Ltmp5)
; CHECK-NEXT: LD a3,0(a3)
; CHECK-NEXT: AND a0,a0,a3
; CHECK-NEXT: ANDI a1,a1,63
; CHECK-NEXT: SRL a0,a0,a1
; CHECK-NEXT: OR a0,a0,a2
; CHECK-NEXT: RET
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
; CHECK-NEXT: RET
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
; CHECK-NEXT: SUB a2,zero,a1
; CHECK-NEXT: SLL a1,a0,a1
; CHECK-NEXT: ANDI a2,a2,63
; CHECK-NEXT: SRL a0,a0,a2
; CHECK-NEXT: OR a0,a1,a0
; CHECK-NEXT: RET
  %z = sub i64 0, %y
  %and = and i64 %z, 63
  %b = shl i64 %x, %y
  %c = lshr i64 %x, %and
  %d = or i64 %b, %c
  ret i64 %d
}