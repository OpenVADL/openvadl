; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im --relocation-model=pic -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT
@external_var = external global i32
@internal_var = internal global i32 42


; external address

define ptr @f1() nounwind {
; CHECK-LABEL: f1: # @f1
; CHECK-LABEL: # %bb.0: # %entry
; CHECK-NEXT: LGA a0,external_var
; CHECK-NEXT: RET
entry:
  ret ptr @external_var
}

define ptr @f2() nounwind {
; CHECK-LABEL: f2: # @f2
; CHECK-LABEL: # %bb.0: # %entry
; CHECK-NEXT: LLA a0,internal_var
; CHECK-NEXT: RET
entry:
  ret ptr @internal_var
}