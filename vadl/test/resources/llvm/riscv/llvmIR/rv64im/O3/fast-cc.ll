; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define fastcc i32 @callee(<16 x i32> %A) nounwind {
; CHECK-LABEL: callee:
; CHECK:       # %bb.0:
; CHECK-NEXT: RET
;
	%B = extractelement <16 x i32> %A, i32 0
	ret i32 %B
}

; With the fastcc, arguments will be passed by a0-a7 and t2-t6.
; The rest will be pushed on the stack.
define i32 @caller(<16 x i32> %A) nounwind {
; CHECK-LABEL: caller:
; CHECK:       # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-80
; CHECK-NEXT: SD ra,72(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: LD t0,136(sp)
; CHECK-NEXT: SD t0,56(sp)
; CHECK-NEXT: LD t0,128(sp)
; CHECK-NEXT: SD t0,48(sp)
; CHECK-NEXT: LD t0,120(sp)
; CHECK-NEXT: SD t0,40(sp)
; CHECK-NEXT: LD t0,112(sp)
; CHECK-NEXT: SD t0,32(sp)
; CHECK-NEXT: LD t0,104(sp)
; CHECK-NEXT: SD t0,24(sp)
; CHECK-NEXT: LD t0,96(sp)
; CHECK-NEXT: SD t0,16(sp)
; CHECK-NEXT: LD t0,88(sp)
; CHECK-NEXT: SD t0,8(sp)
; CHECK-NEXT: LD t0,80(sp)
; CHECK-NEXT: SD t0,0(sp)
; CHECK-NEXT: CALL callee
; CHECK-NEXT: LD ra,72(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,80
; CHECK-NEXT: RET
	%C = call fastcc i32 @callee(<16 x i32> %A)
	ret i32 %C
}