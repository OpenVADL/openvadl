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
; CHECK-NEXT: ADDI sp,sp,-128
; CHECK-NEXT: SD ra,120(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: LD a2,232(sp)
; CHECK-NEXT: SD a2,104(sp)
; CHECK-NEXT: LD a2,224(sp)
; CHECK-NEXT: SD a2,96(sp)
; CHECK-NEXT: LD a2,216(sp)
; CHECK-NEXT: SD a2,88(sp)
; CHECK-NEXT: LD a2,208(sp)
; CHECK-NEXT: SD a2,80(sp)
; CHECK-NEXT: LD a2,200(sp)
; CHECK-NEXT: SD a2,72(sp)
; CHECK-NEXT: LD a2,192(sp)
; CHECK-NEXT: SD a2,64(sp)
; CHECK-NEXT: LD a2,184(sp)
; CHECK-NEXT: SD a2,56(sp)
; CHECK-NEXT: LD a2,176(sp)
; CHECK-NEXT: SD a2,48(sp)
; CHECK-NEXT: LD a2,168(sp)
; CHECK-NEXT: SD a2,40(sp)
; CHECK-NEXT: LD a2,160(sp)
; CHECK-NEXT: SD a2,32(sp)
; CHECK-NEXT: LD a2,152(sp)
; CHECK-NEXT: SD a2,24(sp)
; CHECK-NEXT: LD a2,144(sp)
; CHECK-NEXT: SD a2,16(sp)
; CHECK-NEXT: LD a2,136(sp)
; CHECK-NEXT: SD a2,8(sp)
; CHECK-NEXT: LD a2,128(sp)
; CHECK-NEXT: SD a2,0(sp)
; CHECK-NEXT: LUI ra,%hi(callee)
; CHECK-NEXT: JALR ra,%lo(callee)(ra)
; CHECK-NEXT: LD ra,120(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,128
; CHECK-NEXT: RET
	%C = call fastcc i32 @callee(<16 x i32> %A)
	ret i32 %C
}