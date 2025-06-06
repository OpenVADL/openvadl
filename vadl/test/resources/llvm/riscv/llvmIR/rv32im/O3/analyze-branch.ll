; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

declare void @test_true()
declare void @test_false()

; !0 corresponds to a branch being taken, !1 to not being takne.
!0 = !{!"branch_weights", i32 64, i32 4}
!1 = !{!"branch_weights", i32 4, i32 64}

define void @test_bcc_fallthrough_taken(i32 %in) nounwind {
; CHECK-LABEL: test_bcc_fallthrough_taken: # @test_bcc_fallthrough_taken
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-16
; CHECK-NEXT: SW ra,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: ADDI a1,zero,42
; CHECK-NEXT: BNE a0,a1,.LBB0_2
; CHECK-LABEL: # %bb.1: # %true
; CHECK-NEXT: CALL test_true
; CHECK-NEXT: LW ra,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: RET
; CHECK-LABEL: LBB0_2:
; CHECK-NEXT: CALL test_false
; CHECK-NEXT: LW ra,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: RET
  %tst = icmp eq i32 %in, 42
  br i1 %tst, label %true, label %false, !prof !0

; Expected layout order is: Entry, TrueBlock, FalseBlock
; Entry->TrueBlock is the common path, which should be taken whenever the
; conditional branch is false.

true:
  call void @test_true()
  ret void

false:
  call void @test_false()
  ret void
}

define void @test_bcc_fallthrough_nottaken(i32 %in) nounwind {
; CHECK-LABEL: test_bcc_fallthrough_nottaken: # @test_bcc_fallthrough_nottaken
; CHECK-LABEL: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-16
; CHECK-NEXT: SW ra,12(sp) # 4-byte Folded Spill
; CHECK-NEXT: ADDI a1,zero,42
; CHECK-NEXT: BNE a0,a1,.LBB1_1
; CHECK-NEXT: J .LBB1_2
; CHECK-LABEL: .LBB1_1: # %false
; CHECK-NEXT: CALL test_false
; CHECK-NEXT: LW ra,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: RET
; CHECK-LABEL: .LBB1_2: # %true
; CHECK-NEXT: CALL test_true
; CHECK-NEXT: LW ra,12(sp) # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: RET

  %tst = icmp eq i32 %in, 42
  br i1 %tst, label %true, label %false, !prof !1

; Expected layout order is: Entry, FalseBlock, TrueBlock
; Entry->FalseBlock is the common path, which should be taken whenever the
; conditional branch is false

true:
  call void @test_true()
  ret void

false:
  call void @test_false()
  ret void
}
