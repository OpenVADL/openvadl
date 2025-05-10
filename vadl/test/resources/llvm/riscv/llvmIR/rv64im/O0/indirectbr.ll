; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O0 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define i32 @indirectbr(ptr %target) nounwind {
  ; CHECK-LABEL: indirectbr: # @indirectbr
  ; CHECK-LABEL: # %bb.0:
  ; CHECK-NEXT: JALR zero,0(a0)
  ; CHECK-LABEL: LBB0_1: # %test_label
  ; CHECK: ADDI a0,zero,0
  ; CHECK-NEXT: RET
   indirectbr ptr %target, [label %test_label]
test_label:
  br label %ret
ret:
  ret i32 0
}

define i32 @indirectbr_with_offset(ptr %a) nounwind {
  ; CHECK-LABEL: indirectbr_with_offset: # @indirectbr_with_offset
  ; CHECK-LABEL: # %bb.0:
  ; CHECK-NEXT: JALR zero,1380(a0)
  ; CHECK-LABEL: LBB1_1:
  ; CHECK-NEXT: J .LBB1_2
  ; CHECK-LABEL: LBB1_2:
  ; CHECK-NEXT: ADDI a0,zero,0
  ; CHECK-NEXT: RET

  %target = getelementptr inbounds i8, ptr %a, i32 1380
  indirectbr ptr %target, [label %test_label]
test_label:
  br label %ret
ret:
  ret i32 0
}