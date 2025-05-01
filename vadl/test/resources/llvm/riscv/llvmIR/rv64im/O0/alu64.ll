; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O0 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

; Register-immediate instructions

define i64 @addi(i64 %a) nounwind {
; CHECK-LABEL: addi: # @addi
; CHECK-LABEL: # %bb.0:
; CHECK: ADDI a0,a0,1
; CHECK-NEXT: RET
  %1 = add i64 %a, 1
  ret i64 %1
}

define i64 @slti(i64 %a) nounwind {
; CHECK-LABEL: slti: # @slti
; CHECK-LABEL: # %bb.0:
; CHECK: SLTI a0,a0,2
; CHECK-NEXT: RET
  %1 = icmp slt i64 %a, 2
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @sltiu(i64 %a) nounwind {
; CHECK-LABEL: sltiu: # @sltiu
; CHECK-LABEL: # %bb.0:
; CHECK: SLTIU a0,a0,3
; CHECK-NEXT: RET
  %1 = icmp ult i64 %a, 3
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @xori(i64 %a) nounwind {
; CHECK-LABEL: xori: # @xori
; CHECK-LABEL: # %bb.0:
; CHECK: XORI a0,a0,4
; CHECK-NEXT: RET
  %1 = xor i64 %a, 4
  ret i64 %1
}

define i64 @ori(i64 %a) nounwind {
; CHECK-LABEL: ori: # @ori
; CHECK-LABEL: # %bb.0:
; CHECK: ORI a0,a0,5
; CHECK-NEXT: RET
  %1 = or i64 %a, 5
  ret i64 %1
}

define i64 @andi(i64 %a) nounwind {
; CHECK-LABEL: andi: # @andi
; CHECK-LABEL: # %bb.0:
; CHECK: ANDI a0,a0,6
; CHECK-NEXT: RET
  %1 = and i64 %a, 6
  ret i64 %1
}

define i64 @slli(i64 %a) nounwind {
; CHECK-LABEL: slli: # @slli
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,7
; CHECK-NEXT: RET
  %1 = shl i64 %a, 7
  ret i64 %1
}

define i64 @srli(i64 %a) nounwind {
; CHECK-LABEL: srli: # @srli
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRLI a0,a0,8
; CHECK-NEXT: RET
  %1 = lshr i64 %a, 8
  ret i64 %1
}

define i64 @srai(i64 %a) nounwind {
; CHECK-LABEL: srai: # @srai
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SRAI a0,a0,9
; CHECK-NEXT: RET
  %1 = ashr i64 %a, 9
  ret i64 %1
}

; Register-register instructions

define i64 @add(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: add: # @add
; CHECK-LABEL: # %bb.0:
; CHECK: ADD a0,a0,a1
; CHECK-NEXT: RET
  %1 = add i64 %a, %b
  ret i64 %1
}

define i64 @sub(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sub: # @sub
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: SUB a0,a0,a1
; CHECK-NEXT: RET
  %1 = sub i64 %a, %b
  ret i64 %1
}

define i64 @sll(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sll: # @sll
; CHECK-LABEL: # %bb.0:
; CHECK: SLL a0,a0,a1
; CHECK-NEXT: RET
  %1 = shl i64 %a, %b
  ret i64 %1
}

define i64 @slt(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: slt: # @slt
; CHECK-LABEL: # %bb.0:
; CHECK: SLT a0,a0,a1
; CHECK-NEXT: RET
  %1 = icmp slt i64 %a, %b
  %2 = zext i1 %1 to i64
  ret i64 %2
}

define i64 @xor(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: xor: # @xor
; CHECK-LABEL: # %bb.0:
; CHECK: XOR a0,a0,a1
; CHECK-NEXT: RET
  %1 = xor i64 %a, %b
  ret i64 %1
}

define i64 @srl(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: srl: # @srl
; CHECK-LABEL: # %bb.0:
; CHECK: SRL a0,a0,a1
; CHECK-NEXT: RET
  %1 = lshr i64 %a, %b
  ret i64 %1
}

define i64 @sra(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: sra: # @sra
; CHECK-LABEL: # %bb.0:
; CHECK: SRA a0,a0,a1
; CHECK-NEXT: RET
  %1 = ashr i64 %a, %b
  ret i64 %1
}

define i64 @or(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: or: # @or
; CHECK-LABEL: # %bb.0:
; CHECK: OR a0,a0,a1
; CHECK-NEXT: RET
  %1 = or i64 %a, %b
  ret i64 %1
}

define i64 @and(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: and: # @and
; CHECK-LABEL: # %bb.0:
; CHECK: AND a0,a0,a1
; CHECK-NEXT: RET
  %1 = and i64 %a, %b
  ret i64 %1
}
