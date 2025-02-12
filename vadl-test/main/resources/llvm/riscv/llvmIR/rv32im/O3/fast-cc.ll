; RUN: $LLC -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | $FILECHECK $INPUT

define fastcc i32 @callee(<16 x i32> %A) nounwind {
; CHECK-LABEL: callee:
; CHECK:       # %bb.0:
; CHECK-NEXT: JALR zero,0(ra)
;
	%B = extractelement <16 x i32> %A, i32 0
	ret i32 %B
}

; With the fastcc, arguments will be passed by a0-a7 and t2-t6.
; The rest will be pushed on the stack.
define i32 @caller(<16 x i32> %A) nounwind {
; CHECK-LABEL: caller:
; CHECK:       # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-32
; CHECK-NEXT: LW t0,60(sp)
; CHECK-NEXT: SW t0,28(sp)
; CHECK-NEXT: LW t0,56(sp)
; CHECK-NEXT: SW t0,24(sp)
; CHECK-NEXT: LW t0,52(sp)
; CHECK-NEXT: SW t0,20(sp)
; CHECK-NEXT: LW t0,48(sp)
; CHECK-NEXT: SW t0,16(sp)
; CHECK-NEXT: LW t0,44(sp)
; CHECK-NEXT: SW t0,12(sp)
; CHECK-NEXT: LW t0,40(sp)
; CHECK-NEXT: SW t0,8(sp)
; CHECK-NEXT: LW t0,36(sp)
; CHECK-NEXT: SW t0,4(sp)
; CHECK-NEXT: LW t0,32(sp)
; CHECK-NEXT: SW t0,0(sp)
; CHECK-NEXT: LUI ra,%hi(callee)
; CHECK-NEXT: JALR ra,%lo(callee)(ra)
; CHECK-NEXT: ADDI sp,sp,32
; CHECK-NEXT: JALR zero,0(ra)
	%C = call fastcc i32 @callee(<16 x i32> %A)
	ret i32 %C
}