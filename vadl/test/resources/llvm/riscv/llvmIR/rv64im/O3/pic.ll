; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im --relocation-model=pic -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT
@external_var = external global i32
@internal_var = internal global i32 42


; external address

define ptr @f1() nounwind {
; CHECK-LABEL: f1: # @f1
; CHECK-LABEL: # %bb.0: # %entry
; CHECK-LABEL: .Ltmp0:
; CHECK-NEXT: AUIPC a0,%got_pcrel_hi(external_var)
; CHECK-NEXT: LD a0,%pcrel_lo(.Ltmp0)(a0)
; CHECK-NEXT: RET
entry:
  ret ptr @external_var
}

define ptr @f2() nounwind {
; CHECK-LABEL: f2: # @f2
; CHECK-LABEL: # %bb.0: # %entry
; CHECK-LABEL: .Ltmp1:
; CHECK-NEXT: AUIPC a0,%pcrel_hi(internal_var)
; CHECK-NEXT: ADDI a0,a0,%pcrel_lo(.Ltmp1)
; CHECK-NEXT: RET
entry:
  ret ptr @internal_var
}