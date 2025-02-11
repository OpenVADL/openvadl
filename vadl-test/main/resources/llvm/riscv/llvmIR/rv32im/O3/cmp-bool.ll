; RUN: $LLC -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | $FILECHECK $INPUT

define void @bool_eq(i1 zeroext %a, i1 zeroext %b, ptr nocapture %c) nounwind {
; CHECK-LABEL: bool_eq:
; CHECK: BNE a0,a1,.LBB0_2
; CHECK-NEXT: # %bb.1: # %if.then
; CHECK-NEXT: JALR ra,0(ra)
; CHECK-NEXT: .LBB0_2: # %if.end
; CHECK: JALR zero,0(ra)
entry:
  %0 = xor i1 %a, %b
  br i1 %0, label %if.end, label %if.then

if.then:
  tail call void %c() #1
  br label %if.end

if.end:
  ret void
}

define void @bool_ne(i1 zeroext %a, i1 zeroext %b, ptr nocapture %c) nounwind {
; CHECK-LABEL: bool_ne:
; CHECK: BEQ a0,a1,.LBB0_2
; CHECK-NEXT: # %bb.1: # %if.then
; CHECK-NEXT: JALR ra,0(ra)
; CHECK-NEXT: .LBB0_2: # %if.end
; CHECK: JALR zero,0(ra)
entry:
  %cmp = xor i1 %a, %b
  br i1 %cmp, label %if.then, label %if.end

if.then:
  tail call void %c() #1
  br label %if.end

if.end:
  ret void
}