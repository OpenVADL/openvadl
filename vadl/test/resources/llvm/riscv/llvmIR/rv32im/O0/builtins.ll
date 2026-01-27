; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O0 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT


define i32 @add(i32 %0, i32 %1) {
; CHECK-LABEL: add: # @add
; CHECK:         add a0,a0,a1
; CHECK-NEXT:    RET
  %a = call i32 @llvm.rv32im.ADD.i32(i32 %0, i32 %1)
  ret i32 %a
}

define i32 @sub(i32 %0, i32 %1) {
; CHECK-LABEL: sub: # @sub
; CHECK:         sub a0,a0,a1
; CHECK-NEXT:    RET
  %a = call i32 @llvm.rv32im.SUB.i32(i32 %0, i32 %1)
  ret i32 %a
}

define i32 @mul(i32 %0, i32 %1) {
; CHECK-LABEL: mul: # @mul
; CHECK:         mul a0,a0,a1
; CHECK-NEXT:    RET
  %a = call i32 @llvm.rv32im.MUL.i32(i32 %0, i32 %1)
  ret i32 %a
}