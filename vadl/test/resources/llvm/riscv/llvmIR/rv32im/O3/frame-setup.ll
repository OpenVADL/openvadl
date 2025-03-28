; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define i32 @main() nounwind {
entry:
; CHECK-LABEL: main: # @main
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI a0,zero,-16
; CHECK-NEXT: ADD sp,sp,a0
; CHECK-NEXT: ADDI a0,zero,0
; CHECK-NEXT: ADDI a1,zero,16
; CHECK-NEXT: ADD sp,sp,a1
  %i = alloca i32, align 4
  ret i32 0
}