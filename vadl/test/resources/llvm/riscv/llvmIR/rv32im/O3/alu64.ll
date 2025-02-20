; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

; Register-immediate instructions

define i64 @addi(i64 %a) nounwind {
; CHECK-LABEL: addi: # @addi
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a0,a0,1
; CHECK-NEXT: XOR a2,a0,zero
; CHECK-NEXT: SLTIU a2,a2,1
; CHECK-NEXT: ADD a1,a1,a2
; CHECK-NEXT: JALR zero,0(ra)
  %1 = add i64 %a, 1
  ret i64 %1
}

define i64 @slti(i64 %a) nounwind {
; CHECK-LABEL: slti: # @slti
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a2,a1,0
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: BEQ a2,a1,.LBB1_2
; CHECK-LABEL: # %bb.1:
; CHECK-NEXT: SLT a0,a2,zero
; CHECK-NEXT: JALR zero,0(ra)
; CHECK-LABEL: .LBB1_2:
; CHECK-NEXT: SLTIU a0,a0,2
; CHECK-NEXT: JALR zero,0(ra)
  %1 = icmp slt i64 %a, 2
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @sltiu(i64 %a) nounwind {
; CHECK-LABEL: sltiu: # @sltiu
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a2,a1,0
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: BEQ a2,a1,.LBB2_2
; CHECK-LABEL: # %bb.1:
; CHECK-NEXT: ADDI a0,a1,0
; CHECK-NEXT: JALR zero,0(ra)
; CHECK-LABEL: .LBB2_2:
; CHECK-NEXT: SLTIU a0,a0,3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = icmp ult i64 %a, 3
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @xori(i64 %a) nounwind {
; CHECK-LABEL: xori: # @xori
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: XORI a0,a0,4
; CHECK-NEXT: JALR zero,0(ra)
  %1 = xor i64 %a, 4
  ret i64 %1
}

define i64 @ori(i64 %a) nounwind {
; CHECK-LABEL: ori: # @ori
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ORI a0,a0,5
; CHECK-NEXT: JALR zero,0(ra)
  %1 = or i64 %a, 5
  ret i64 %1
}

define i64 @andi(i64 %a) nounwind {
; CHECK-LABEL: andi: # @andi
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ANDI a0,a0,6
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = and i64 %a, 6
  ret i64 %1
}

define i64 @slli(i64 %a) nounwind {
; CHECK-LABEL: slli: # @slli
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLLI a1,a1,7
; CHECK-NEXT: SRLI a2,a0,25
; CHECK-NEXT: OR a1,a1,a2
; CHECK-NEXT: SLLI a0,a0,7
; CHECK-NEXT: JALR zero,0(ra)
  %1 = shl i64 %a, 7
  ret i64 %1
}

define i64 @srli(i64 %a) nounwind {
; CHECK-LABEL: srli: # @srli
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRLI a0,a0,8
; CHECK-NEXT: SLLI a2,a1,24
; CHECK-NEXT: OR a0,a0,a2
; CHECK-NEXT: SRLI a1,a1,8
; CHECK-NEXT: JALR zero,0(ra)
  %1 = lshr i64 %a, 8
  ret i64 %1
}

define i64 @srai(i64 %a) nounwind {
; CHECK-LABEL: srai: # @srai
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRLI a0,a0,9
; CHECK-NEXT: SLLI a2,a1,23
; CHECK-NEXT: OR a0,a0,a2
; CHECK-NEXT: SRAI a1,a1,9
; CHECK-NEXT: JALR zero,0(ra)
  %1 = ashr i64 %a, 9
  ret i64 %1
}

; Register-register instructions

define i64 @add(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: add: # @add
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADD a1,a1,a3
; CHECK-NEXT: ADD a2,a0,a2
; CHECK-NEXT: SLTU a0,a2,a0
; CHECK-NEXT: ADD a1,a1,a0
; CHECK-NEXT: ADDI a0,a2,0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = add i64 %a, %b
  ret i64 %1
}

define i64 @sub(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sub: # @sub
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SUB a1,a1,a3
; CHECK-NEXT: SLTU a3,a0,a2
; CHECK-NEXT: SUB a1,a1,a3
; CHECK-NEXT: SUB a0,a0,a2
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sub i64 %a, %b
  ret i64 %1
}

define i64 @sll(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sll: # @sll
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-16
; CHECK-NEXT: SW ra,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: LUI ra,%hi(__ashldi3)
; CHECK-NEXT: JALR ra,%lo(__ashldi3)(ra)
; CHECK-NEXT: LW ra,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: JALR zero,0(ra)
  %1 = shl i64 %a, %b
  ret i64 %1
}

define i64 @slt(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: slt: # @slt
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: BEQ a1,a3,.LBB12_2
; CHECK-LABEL: # %bb.1:
; CHECK-NEXT: SLT a0,a1,a3
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: JALR zero,0(ra)
; CHECK-LABEL: .LBB12_2:
; CHECK-NEXT: SLTU a0,a0,a2
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = icmp slt i64 %a, %b
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @sltu(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sltu: # @sltu
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: BEQ a1,a3,.LBB13_2
; CHECK-LABEL: # %bb.1:
; CHECK-NEXT: SLTU a0,a1,a3
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: JALR zero,0(ra)
; CHECK-LABEL: .LBB13_2:
; CHECK-NEXT: SLTU a0,a0,a2
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = icmp ult i64 %a, %b
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @xor(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: xor: # @xor
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: XOR a0,a0,a2
; CHECK-NEXT: XOR a1,a1,a3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = xor i64 %a, %b
  ret i64 %1
}

define i64 @srl(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: srl: # @srl
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-16
; CHECK-NEXT: SW ra,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: LUI ra,%hi(__lshrdi3)
; CHECK-NEXT: JALR ra,%lo(__lshrdi3)(ra)
; CHECK-NEXT: LW ra,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: JALR zero,0(ra)
  %1 = lshr i64 %a, %b
  ret i64 %1
}

define i64 @sra(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sra: # @sra
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-16
; CHECK-NEXT: SW ra,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: LUI ra,%hi(__ashrdi3)
; CHECK-NEXT: JALR ra,%lo(__ashrdi3)(ra)
; CHECK-NEXT: LW ra,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: JALR zero,0(ra)
  %1 = ashr i64 %a, %b
  ret i64 %1
}

define i64 @or(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: or: # @or
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: OR a0,a0,a2
; CHECK-NEXT: OR a1,a1,a3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = or i64 %a, %b
  ret i64 %1
}

define i64 @and(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: and: # @and
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: AND a0,a0,a2
; CHECK-NEXT: AND a1,a1,a3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = and i64 %a, %b
  ret i64 %1
}

; RV64I-only instructions

define signext i32 @addiw(i32 signext %a) nounwind {
; CHECK-LABEL: addiw: # @addiw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a0,a0,123
; CHECK-NEXT: JALR zero,0(ra)
  %1 = add i32 %a, 123
  ret i32 %1
}

define signext i32 @slliw(i32 signext %a) nounwind {
; CHECK-LABEL: slliw: # @slliw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,17
; CHECK-NEXT: JALR zero,0(ra)
  %1 = shl i32 %a, 17
  ret i32 %1
}

define signext i32 @srliw(i32 %a) nounwind {
; CHECK-LABEL: srliw: # @srliw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRLI a0,a0,8
; CHECK-NEXT: JALR zero,0(ra)
  %1 = lshr i32 %a, 8
  ret i32 %1
}

define signext i32 @sraiw(i32 %a) nounwind {
; CHECK-LABEL: sraiw: # @sraiw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRAI a0,a0,9
; CHECK-NEXT: JALR zero,0(ra)
  %1 = ashr i32 %a, 9
  ret i32 %1
}

define i64 @sraiw_i64(i64 %a) nounwind {
; CHECK-LABEL: sraiw_i64: # @sraiw_i64
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRAI a2,a0,9
; CHECK-NEXT: SRAI a1,a0,31
; CHECK-NEXT: ADDI a0,a2,0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = shl i64 %a, 32
  %2 = ashr i64 %1, 41
  ret i64 %2
}

define signext i32 @sextw(i32 zeroext %a) nounwind {
; CHECK-LABEL: sextw: # @sextw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: JALR zero,0(ra)
  ret i32 %a
}

define signext i32 @addw(i32 signext %a, i32 signext %b) nounwind {
; CHECK-LABEL: addw: # @addw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADD a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = add i32 %a, %b
  ret i32 %1
}

define signext i32 @subw(i32 signext %a, i32 signext %b) nounwind {
; CHECK-LABEL: subw: # @subw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SUB a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sub i32 %a, %b
  ret i32 %1
}

define signext i32 @sllw(i32 signext %a, i32 zeroext %b) nounwind {
; CHECK-LABEL: sllw: # @sllw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLL a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = shl i32 %a, %b
  ret i32 %1
}

define signext i32 @srlw(i32 signext %a, i32 zeroext %b) nounwind {
; CHECK-LABEL: srlw: # @srlw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRL a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = lshr i32 %a, %b
  ret i32 %1
}

define signext i32 @sraw(i64 %a, i32 zeroext %b) nounwind {
; CHECK-LABEL: sraw: # @sraw
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRA a0,a0,a2
; CHECK-NEXT: JALR zero,0(ra)
  %1 = trunc i64 %a to i32
  %2 = ashr i32 %1, %b
  ret i32 %2
}

define i64 @add_hi_and_lo_negone(i64 %0) {
; CHECK-LABEL: add_hi_and_lo_negone: # @add_hi_and_lo_negone
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: XOR a2,a0,zero
; CHECK-NEXT: SLTIU a2,a2,1
; CHECK-NEXT: SUB a1,a1,a2
; CHECK-NEXT: ADDI a0,a0,-1
; CHECK-NEXT: JALR zero,0(ra)
  %2 = add nsw i64 %0, -1
  ret i64 %2
}

define i64 @add_hi_zero_lo_negone(i64 %0) {
; CHECK-LABEL: add_hi_zero_lo_negone: # @add_hi_zero_lo_negone
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: XORI a2,a0,0
; CHECK-NEXT: SLTU a2,zero,a2
; CHECK-NEXT: ADD a1,a1,a2
; CHECK-NEXT: ADDI a0,a0,-1
; CHECK-NEXT: JALR zero,0(ra)
  %2 = add i64 %0, 4294967295
  ret i64 %2
}

define i64 @add_lo_negone(i64 %0) {
; CHECK-LABEL: add_lo_negone: # @add_lo_negone
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: XORI a2,a0,0
; CHECK-NEXT: SLTU a2,zero,a2
; CHECK-NEXT: ADD a1,a1,a2
; CHECK-NEXT: ADDI a1,a1,-2
; CHECK-NEXT: ADDI a0,a0,-1
; CHECK-NEXT: JALR zero,0(ra)
  %2 = add nsw i64 %0, -4294967297
  ret i64 %2
}

define i64 @add_hi_one_lo_negone(i64 %0) {
; CHECK-LABEL: add_hi_one_lo_negone: # @add_hi_one_lo_negone
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: XORI a2,a0,0
; CHECK-NEXT: SLTU a2,zero,a2
; CHECK-NEXT: ADD a1,a1,a2
; CHECK-NEXT: ADDI a1,a1,1
; CHECK-NEXT: ADDI a0,a0,-1
; CHECK-NEXT: JALR zero,0(ra)
  %2 = add nsw i64 %0, 8589934591
  ret i64 %2
}
