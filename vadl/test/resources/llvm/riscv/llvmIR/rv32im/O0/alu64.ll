; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O0 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

; Register-immediate instructions

define i64 @addi(i64 %a) nounwind {
; CHECK-LABEL: addi: # @addi
; CHECK-LABEL: # %bb.0:
; CHECK: ADDI a0,a0,1
; CHECK-NEXT: ADDI a2,zero,0
; CHECK-NEXT: XOR a2,a0,a2
; CHECK-NEXT: SLTIU a2,a2,1
; CHECK-NEXT: ADD a1,a1,a2
; CHECK-NEXT: JALR zero,0(ra)
  %1 = add i64 %a, 1
  ret i64 %1
}

define i64 @slti(i64 %a) nounwind {
; CHECK-LABEL: slti: # @slti
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,-16
; CHECK-NEXT: ADD sp,sp,a2
; CHECK: SLTI a2,a1,0
; CHECK-NEXT: SW a2,4(sp) # 4-byte Folded Spill
; CHECK-NEXT: SLTIU a2,a0,2
  %1 = icmp slt i64 %a, 2
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @sltiu(i64 %a) nounwind {
; CHECK-LABEL: sltiu: # @sltiu
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a2,zero,-16
; CHECK-NEXT: ADD sp,sp,a2
; CHECK: SLTIU a2,a0,3
; CHECK-NEXT: ADDI a0,zero,0
; CHECK-NEXT: SW a0,8(sp) # 4-byte Folded Spill
; CHECK-NEXT: SW a2,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: BEQ a1,a0,.LBB2_2
; CHECK-LABEL: # %bb.1:
; CHECK-NEXT: LW a0,8(sp) # 4-byte Folded Reload
; CHECK-NEXT: SW a0,12(sp) # 4-byte Folded Spill
; CHECK-LABEL: .LBB2_2:
; CHECK-NEXT: LW a1,8(sp) # 4-byte Folded Reload
; CHECK-NEXT: LW a0,12(sp) # 4-byte Folded Reload
  %1 = icmp ult i64 %a, 3
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @xori(i64 %a) nounwind {
; CHECK-LABEL: xori: # @xori
; CHECK-LABEL: # %bb.0:
; CHECK: XORI a0,a0,4
; CHECK-NEXT: JALR zero,0(ra)
  %1 = xor i64 %a, 4
  ret i64 %1
}

define i64 @ori(i64 %a) nounwind {
; CHECK-LABEL: ori: # @ori
; CHECK-LABEL: # %bb.0:
; CHECK: ORI a0,a0,5
; CHECK-NEXT: JALR zero,0(ra)
  %1 = or i64 %a, 5
  ret i64 %1
}

define i64 @andi(i64 %a) nounwind {
; CHECK-LABEL: andi: # @andi
; CHECK-LABEL: # %bb.0:
; CHECK: ANDI a0,a0,6
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = and i64 %a, 6
  ret i64 %1
}

define i64 @slli(i64 %a) nounwind {
; CHECK-LABEL: slli: # @slli
; CHECK-LABEL: # %bb.0:
; CHECK: SRLI a2,a0,25
; CHECK-NEXT: SLLI a1,a1,7
; CHECK-NEXT: OR a1,a1,a2
; CHECK-NEXT: SLLI a0,a0,7
; CHECK-NEXT: JALR zero,0(ra)
  %1 = shl i64 %a, 7
  ret i64 %1
}

define i64 @srli(i64 %a) nounwind {
; CHECK-LABEL: srli: # @srli
; CHECK-LABEL: # %bb.0:
; CHECK: SLLI a2,a1,24
; CHECK-NEXT: SRLI a0,a0,8
; CHECK-NEXT: OR a0,a0,a2
; CHECK-NEXT: SRLI a1,a1,8
; CHECK-NEXT: JALR zero,0(ra)
  %1 = lshr i64 %a, 8
  ret i64 %1
}

define i64 @srai(i64 %a) nounwind {
; CHECK-LABEL: srai: # @srai
; CHECK-LABEL: # %bb.0:
; CHECK: SLLI a2,a1,23
; CHECK-NEXT: SRLI a0,a0,9
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
; CHECK-NEXT: ADDI a4,zero,-16
; CHECK-NEXT: ADD sp,sp,a4
; CHECK-NEXT: SW a2,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: ADDI a2,a0,0
; CHECK-NEXT: LW a0,12(sp) # 4-byte Folded Reload
; CHECK: ADD a1,a1,a3
; CHECK-NEXT: ADD a0,a2,a0
; CHECK-NEXT: SLTU a2,a0,a2
; CHECK-NEXT: ADD a1,a1,a2
  %1 = add i64 %a, %b
  ret i64 %1
}

define i64 @sub(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sub: # @sub
; CHECK-LABEL: # %bb.0:
; CHECK: ADDI a4,a3,0
; CHECK: SLTU a3,a0,a2
; CHECK-NEXT: SUB a1,a1,a4
; CHECK-NEXT: SUB a1,a1,a3
; CHECK-NEXT: SUB a0,a0,a2
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sub i64 %a, %b
  ret i64 %1
}

define i64 @sll(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sll: # @sll
; CHECK-LABEL: # %bb.0:
; CHECK: LUI ra,%hi(__ashldi3)
; CHECK-NEXT: JALR ra,%lo(__ashldi3)(ra)
; CHECK-NEXT: LW ra,12(sp)
  %1 = shl i64 %a, %b
  ret i64 %1
}

define i64 @slt(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: slt: # @slt
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a4,zero,-16
; CHECK-NEXT: ADD sp,sp,a4
; CHECK: SLT a4,a1,a3
; CHECK-NEXT: SW a4,8(sp) # 4-byte Folded Spill
; CHECK-NEXT: SLTU a0,a0,a2
; CHECK-NEXT: SW a0,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: BEQ a1,a3,.LBB12_2
; CHECK-LABEL: # %bb.1:
; CHECK-NEXT: LW a0,8(sp) # 4-byte Folded Reload
; CHECK-NEXT: SW a0,12(sp) # 4-byte Folded Spill
; CHECK-LABEL: .LBB12_2:
; CHECK-NEXT: LW a0,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI a1,zero,0
  %1 = icmp slt i64 %a, %b
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @xor(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: xor: # @xor
; CHECK-LABEL: # %bb.0:
; CHECK: XOR a0,a0,a2
; CHECK-NEXT: XOR a1,a1,a3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = xor i64 %a, %b
  ret i64 %1
}

define i64 @srl(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: srl: # @srl
; CHECK-LABEL: # %bb.0:
; CHECK: ADDI a3,zero,-16
; CHECK-NEXT: ADD sp,sp,a3
; CHECK: LUI ra,%hi(__lshrdi3)
; CHECK-NEXT: JALR ra,%lo(__lshrdi3)(ra)
; CHECK-NEXT: LW ra,12(sp)
  %1 = lshr i64 %a, %b
  ret i64 %1
}

define i64 @sra(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sra: # @sra
; CHECK-LABEL: # %bb.0:
; CHECK: ADDI a3,zero,-16
; CHECK-NEXT: ADD sp,sp,a3
; CHECK: LUI ra,%hi(__ashrdi3)
; CHECK-NEXT: JALR ra,%lo(__ashrdi3)(ra)
; CHECK-NEXT: LW ra,12(sp)
  %1 = ashr i64 %a, %b
  ret i64 %1
}

define i64 @or(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: or: # @or
; CHECK-LABEL: # %bb.0:
; CHECK: OR a0,a0,a2
; CHECK-NEXT: OR a1,a1,a3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = or i64 %a, %b
  ret i64 %1
}

define i64 @and(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: and: # @and
; CHECK-LABEL: # %bb.0:
; CHECK: AND a0,a0,a2
; CHECK-NEXT: AND a1,a1,a3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = and i64 %a, %b
  ret i64 %1
}
