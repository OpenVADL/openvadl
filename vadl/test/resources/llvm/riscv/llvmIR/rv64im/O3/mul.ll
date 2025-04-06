; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define signext i32 @square(i32 %a) nounwind {
; CHECK-LABEL: square: # @square
; CHECK-LABEL: # %bb.0:
; CHECK: MUL a0,a0,a0
; CHECK-NEXT: SLLI a0,a0,32
; CHECK-NEXT: SRAI a0,a0,32
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i32 %a, %a
  ret i32 %1
}

define signext i32 @mul(i32 %a, i32 %b) nounwind {
; CHECK-LABEL: mul: # @mul
; CHECK-LABEL: # %bb.0:
; CHECK: MUL a0,a0,a1
; CHECK-NEXT: SLLI a0,a0,32
; CHECK-NEXT: SRAI a0,a0,32
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i32 %a, %b
  ret i32 %1
}

define signext i32 @mul_constant(i32 %a) nounwind {
; CHECK-LABEL: mul_constant: # @mul_constant
; CHECK-LABEL: # %bb.0:
; CHECK: LUI a1,0x0
; CHECK-NEXT: ADDI a1,a1,5
; CHECK-NEXT: SLLI a1,a1,16
; CHECK-NEXT: ORI a1,a1,0
; CHECK-NEXT: SLLI a1,a1,16
; CHECK-NEXT: ORI a1,a1,0
; CHECK-NEXT: MUL a0,a0,a1
; CHECK-NEXT: SRAI a0,a0,32
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i32 %a, 5
  ret i32 %1
}

define i32 @mul_pow2(i32 %a) nounwind {
; CHECK-LABEL: mul_pow2: # @mul_pow2
; CHECK-LABEL: # %bb.0:
; CHECK: SLLI a0,a0,3
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i32 %a, 8
  ret i32 %1
}

define i64 @mul64(i64 %a, i64 %b) nounwind {
; CHECK-LABEL: mul64: # @mul64
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: MUL a0,a0,a1
; CHECK-NEXT: JALR zero,0(ra)
  %1 = mul i64 %a, %b
  ret i64 %1
}