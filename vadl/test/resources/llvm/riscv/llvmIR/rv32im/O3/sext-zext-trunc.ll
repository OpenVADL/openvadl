; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT


; The code is different upstream. However, z3 says that it is fine.
;
;from z3 import *
;
;a0 = ZeroExt(31, BitVecVal(1, 1))
;zero = BitVecVal(0, 32)
;
;upstream = (a0 << 31) >> 31
;lcb = zero - (a0 & 1)

;def prove(f):
;    s = Solver()
;    s.add(Not(f))
;    if s.check() == unsat:
;        print ("proved")
;    else:
;        print ("failed to prove")
;
;prove(upstream == lcb)


define i8 @sext_i1_to_i8(i1 %a) nounwind {
; CHECK-LABEL: sext_i1_to_i8:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: ANDI a0,a0,1
; CHECK-NEXT: SUB a0,zero,a0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i1 %a to i8
  ret i8 %1
}

define i16 @sext_i1_to_i16(i1 %a) nounwind {
; CHECK-LABEL: sext_i1_to_i16:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: ANDI a0,a0,1
; CHECK-NEXT: SUB a0,zero,a0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i1 %a to i16
  ret i16 %1
}

define i32 @sext_i1_to_i32(i1 %a) nounwind {
; CHECK-LABEL: sext_i1_to_i32:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: ANDI a0,a0,1
; CHECK-NEXT: SUB a0,zero,a0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i1 %a to i32
  ret i32 %1
}

define i64 @sext_i1_to_i64(i1 %a) nounwind {
; CHECK-LABEL: sext_i1_to_i64:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: ANDI a0,a0,1
; CHECK-NEXT: SUB a0,zero,a0
; CHECK-NEXT: ADDI a1,a0,0
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i1 %a to i64
  ret i64 %1
}

define i16 @sext_i8_to_i16(i8 %a) nounwind {
; CHECK-LABEL: sext_i8_to_i16:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,24
; CHECK-NEXT: SRAI a0,a0,24
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i8 %a to i16
  ret i16 %1
}

define i32 @sext_i8_to_i32(i8 %a) nounwind {
; CHECK-LABEL: sext_i8_to_i32:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: SLLI a0,a0,24
; CHECK-NEXT: SRAI a0,a0,24
; CHECK-NEXT: JALR zero,0(ra)
  %1 = sext i8 %a to i32
  ret i32 %1
}