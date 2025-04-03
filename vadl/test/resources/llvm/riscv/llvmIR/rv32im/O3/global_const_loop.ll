; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

@global = dso_local local_unnamed_addr global i32 0, align 4

define dso_local i32 @foo(i32 noundef %i) local_unnamed_addr #0 {
; CHECK-LABEL: foo:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-16
; CHECK-NEXT: SW ra,12(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW fp,8(sp)                             # 4-byte Folded Spill
; CHECK-NEXT: ADDI fp,sp,16
; CHECK-NEXT: ADDI a1,zero,999
; CHECK-NEXT: BLT a1,a0,.LBB0_2
; CHECK-LABEL: .LBB0_1:                                # %while.body.us
; CHECK-NEXT: # =>This Inner Loop Header: Depth=1
; CHECK-NEXT: JAL zero,.LBB0_1
; CHECK-LABEL: .LBB0_2:                                # %while.end
; CHECK-NEXT: LUI a0,%hi(global)
; CHECK-NEXT: ADDI a0,a0,%lo(global)
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: LW fp,8(sp)                             # 4-byte Folded Reload
; CHECK-NEXT: LW ra,12(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: JALR zero,0(ra)
entry:
  %cmp = icmp slt i32 %i, 1000
  br i1 %cmp, label %while.body.us, label %while.end

while.body.us:                                    ; preds = %entry, %while.body.us
  br label %while.body.us

while.end:                                        ; preds = %entry
  %0 = load i32, ptr @global, align 4, !tbaa !3
  ret i32 %0
}

attributes #0 = { nofree norecurse nosync nounwind memory(read, argmem: none, inaccessiblemem: none) "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" }

!llvm.module.flags = !{!0, !1}
!llvm.ident = !{!2}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"frame-pointer", i32 2}
!2 = !{!"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"}
!3 = !{!4, !4, i64 0}
!4 = !{!"int", !5, i64 0}
!5 = !{!"omnipotent char", !6, i64 0}
!6 = !{!"Simple C/C++ TBAA"}