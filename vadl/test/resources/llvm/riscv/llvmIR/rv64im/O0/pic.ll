; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im --relocation-model=pic -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT
@external_var = external global i32
@internal_var = internal global i32 42


; external address

define ptr @f1() nounwind {
; CHECK-LABEL: f1: # @f1
; CHECK-LABEL: # %bb.0: # %entry
; CHECK-NEXT: AUIPC a0,%got_hi(external_var)
; CHECK-NEXT: LW a0,%pcrel_lo(external_var)(a0)
; CHECK-NEXT: JALR zero,0(ra)
entry:
  ret ptr @external_var
}

define ptr @f2() nounwind {
; CHECK-LABEL: f2: # @f2
; CHECK-LABEL: # %bb.0: # %entry
; CHECK-NEXT: AUIPC a0,%pcrel_hi(internal_var)
; CHECK-NEXT: ADDI a0,a0,%pcrel_lo(internal_var)
; CHECK-NEXT: JALR zero,0(ra)
entry:
  ret ptr @internal_var
}