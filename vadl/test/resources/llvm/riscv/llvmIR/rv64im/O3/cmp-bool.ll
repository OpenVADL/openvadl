; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define void @bool_eq(i1 zeroext %a, i1 zeroext %b, ptr nocapture %c) nounwind {
; CHECK-LABEL: bool_eq:
; CHECK: ADDI sp,sp,-16
; CHECK-NEXT: SD ra,8(sp) # 8-byte Folded Spill
; CHECK: BNE a0,a1,.LBB0_2
; CHECK-NEXT: # %bb.1: # %if.then
; CHECK-NEXT: JALR ra,0(a2)
; CHECK-NEXT: .LBB0_2: # %if.end
; CHECK-NEXT: LD ra,8(sp) # 8-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: RET
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
; CHECK-NEXT: # %bb.0: # %entry
; CHECK: ADDI sp,sp,-16
; CHECK-NEXT: SD ra,8(sp) # 8-byte Folded Spill
; CHECK: BEQ a0,a1,.LBB1_2
; CHECK-NEXT: # %bb.1: # %if.then
; CHECK-NEXT: JALR ra,0(a2)
; CHECK-NEXT: .LBB1_2: # %if.end
; CHECK-NEXT: LD ra,8(sp) # 8-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: RET
entry:
  %cmp = xor i1 %a, %b
  br i1 %cmp, label %if.then, label %if.end

if.then:
  tail call void %c() #1
  br label %if.end

if.end:
  ret void
}