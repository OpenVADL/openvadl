; RUN: $LLC -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | $FILECHECK $INPUT

define signext i32 @square(i32 %a) nounwind {
; CHECK-LABEL: square: # @square
; CHECK-LABEL: # %bb.0:
; CHECK: MUL a0,a0,a0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i32 %a, %a
  ret i32 %1
}

define signext i32 @mul(i32 %a, i32 %b) nounwind {
; CHECK-LABEL: mul: # @mul
; CHECK-LABEL: # %bb.0:
; CHECK: MUL a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i32 %a, %b
  ret i32 %1
}

define signext i32 @mul_constant(i32 %a) nounwind {
; CHECK-LABEL: mul_constant: # @mul_constant
; CHECK-LABEL: # %bb.0:
; CHECK: ADDI a1,zero,5
; CHECK-NEXT: MUL a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i32 %a, 5
  ret i32 %1
}

define i32 @mul_pow2(i32 %a) nounwind {
; CHECK-LABEL: mul_pow2: # @mul_pow2
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i32 %a, 8
  ret i32 %1
}

define i64 @mul64(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: mul64: # @mul64
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: MUL a3,a0,a3
; CHECK-NEXT: MULHU a4,a0,a2
; CHECK-NEXT: ADD a3,a4,a3
; CHECK-NEXT: MUL a1,a1,a2
; CHECK-NEXT: ADD a1,a3,a1
; CHECK-NEXT: MUL a0,a0,a2
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i64 %a, %b
  ret i64 %1
}

define i64 @mul64_constant(i64 %a) nounwind {
; CHECK-LABEL: mul64_constant: # @mul64_constant
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,5
; CHECK-NEXT: MUL a1,a1,a2
; CHECK-NEXT: MULHU a3,a0,a2
; CHECK-NEXT: ADD a1,a3,a1
; CHECK-NEXT: MUL a0,a0,a2
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i64 %a, 5
  ret i64 %1
}

define i32 @mulhs(i32 %a, i32 %b) nounwind {
; CHECK-LABEL: mulhs: # @mulhs
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: MULH a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i32 %a to i64
  %2 = sext i32 %b to i64
  %3 = mul i64 %1, %2
  %4 = lshr i64 %3, 32
  %5 = trunc i64 %4 to i32
  ret i32 %5
}

define i32 @mulhs_positive_constant(i32 %a) nounwind {
; CHECK-LABEL: mulhs_positive_constant: # @mulhs_positive_constant
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a1,zero,5
; CHECK-NEXT: MULH a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i32 %a to i64
  %2 = mul i64 %1, 5
  %3 = lshr i64 %2, 32
  %4 = trunc i64 %3 to i32
  ret i32 %4
}

define i32 @mulhs_negative_constant(i32 %a) nounwind {
; CHECK-LABEL: mulhs_negative_constant: # @mulhs_negative_constant
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a1,zero,-5
; CHECK-NEXT: MULH a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i32 %a to i64
  %2 = mul i64 %1, -5
  %3 = lshr i64 %2, 32
  %4 = trunc i64 %3 to i32
  ret i32 %4
}

define zeroext i32 @mulhu(i32 zeroext %a, i32 zeroext %b) nounwind {
; CHECK-LABEL: mulhu: # @mulhu
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: MULHU a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = zext i32 %a to i64
  %2 = zext i32 %b to i64
  %3 = mul i64 %1, %2
  %4 = lshr i64 %3, 32
  %5 = trunc i64 %4 to i32
  ret i32 %5
}

define i32 @mulhsu(i32 %a, i32 %b) nounwind {
; CHECK-LABEL: mulhsu: # @mulhsu
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,31
; CHECK-NEXT: SRA a2,a1,a2
; CHECK-NEXT: MULHU a1,a0,a1
; CHECK-NEXT: MUL a0,a0,a2
; CHECK-NEXT: ADD a0,a1,a0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = zext i32 %a to i64
  %2 = sext i32 %b to i64
  %3 = mul i64 %1, %2
  %4 = lshr i64 %3, 32
  %5 = trunc i64 %4 to i32
  ret i32 %5
}

define i32 @mulhu_constant(i32 %a) nounwind {
; CHECK-LABEL: mulhu_constant: # @mulhu_constant
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a1,zero,5
; CHECK-NEXT: MULHU a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = zext i32 %a to i64
  %2 = mul i64 %1, 5
  %3 = lshr i64 %2, 32
  %4 = trunc i64 %3 to i32
  ret i32 %4
}

define i8 @muladd_demand(i8 %x, i8 %y) nounwind {
; CHECK-LABEL: muladd_demand: # @muladd_demand
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,1
; CHECK-NEXT: SUB a0,a1,a0
; CHECK-NEXT: ANDI a0,a0,15
; CHECK-NEXT: JALR zero,0(ra)
  %m = mul i8 %x, 14
  %a = add i8 %y, %m
  %r = and i8 %a, 15
  ret i8 %r
}

define i8 @mulsub_demand(i8 %x, i8 %y) nounwind {
; CHECK-LABEL: mulsub_demand: # @mulsub_demand
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,1
; CHECK-NEXT: ADD a0,a1,a0
; CHECK-NEXT: ANDI a0,a0,15
; CHECK-NEXT: JALR zero,0(ra)
  %m = mul i8 %x, 14
  %a = sub i8 %y, %m
  %r = and i8 %a, 15
  ret i8 %r
}

define i8 @muladd_demand_2(i8 %x, i8 %y) nounwind {
; CHECK-LABEL: muladd_demand_2: # @muladd_demand_2
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,1
; CHECK-NEXT: SUB a0,a1,a0
; CHECK-NEXT: ORI a0,a0,-16
; CHECK-NEXT: JALR zero,0(ra)
  %m = mul i8 %x, 14
  %a = add i8 %y, %m
  %r = or i8 %a, 240
  ret i8 %r
}

define i8 @mulsub_demand_2(i8 %x, i8 %y) nounwind {
; CHECK-LABEL: mulsub_demand_2: # @mulsub_demand_2
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,1
; CHECK-NEXT: ADD a0,a1,a0
; CHECK-NEXT: ORI a0,a0,-16
; CHECK-NEXT: JALR zero,0(ra)
  %m = mul i8 %x, 14
  %a = sub i8 %y, %m
  %r = or i8 %a, 240
  ret i8 %r
}

define i64 @muland_demand(i64 %x) nounwind {
; CHECK-LABEL: muland_demand: # @muland_demand
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: LUI a2,0x40000
; CHECK-NEXT: ADDI a2,a2,-1
; CHECK-NEXT: AND a1,a1,a2
; CHECK-NEXT: ADDI a2,zero,12
; CHECK-NEXT: MUL a1,a1,a2
; CHECK-NEXT: ANDI a0,a0,-8
; CHECK-NEXT: MULHU a3,a0,a2
; CHECK-NEXT: ADD a1,a3,a1
; CHECK-NEXT: MUL a0,a0,a2
; CHECK-NEXT: JALR zero,0(ra)
  %and = and i64 %x, 4611686018427387896
  %mul = mul i64 %and, 12
  ret i64 %mul
}

define i64 @mulzext_demand(i32 signext %x) nounwind {
; CHECK-LABEL: mulzext_demand: # @mulzext_demand
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a1,zero,3
; CHECK-NEXT: MUL a1,a0,a1
; CHECK-NEXT: ADDI a0,zero,0
; CHECK-NEXT: JALR zero,0(ra)
  %ext = zext i32 %x to i64
  %mul = mul i64 %ext, 12884901888
  ret i64 %mul
}
