; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O3 --relocation-model=pic -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define void @u_case1_a(ptr %a, i32 signext %b, ptr %c, ptr %d) {
; CHECK-LABEL: u_case1_a:
; CHECK:       # %bb.0:
; CHECK-NEXT:    ADDI a4,zero,32
; CHECK-NEXT:    SW a4,0(a0)
; CHECK-NEXT:    ADDI a0,zero,31
; CHECK-NEXT:    BLTU a0,a1,.LBB0_2
; CHECK-NEXT:  # %bb.1: # %block1
; CHECK-NEXT:    SW a1,0(a2)
; CHECK-NEXT:    RET
; CHECK-NEXT:  .LBB0_2: # %block2
; CHECK-NEXT:    ADDI a0,zero,87
; CHECK-NEXT:    SW a0,0(a3)
; CHECK-NEXT:    RET
  store i32 32, ptr %a
  %p = icmp ule i32 %b, 31
  br i1 %p, label %block1, label %block2

block1:                                           ; preds = %0
  store i32 %b, ptr %c
  br label %end_block

block2:                                           ; preds = %0
  store i32 87, ptr %d
  br label %end_block

end_block:                                        ; preds = %block2, %block1
  ret void
}

define void @case1_a(ptr %a, i32 signext %b, ptr %c, ptr %d) {
; CHECK-LABEL: case1_a:
; CHECK:       # %bb.0:
; CHECK-LABEL: .Ltmp0:
; CHECK-NEXT:    AUIPC a4,%pcrel_hi(.LCPI1_0)
; CHECK-NEXT:    ADDI a4,a4,%pcrel_lo(.Ltmp0)
; CHECK-NEXT:    LD a4,0(a4)
; CHECK-NEXT:    SW a4,0(a0)
; CHECK-NEXT:    ADDI a0,zero,-2
; CHECK-NEXT:    BLT a0,a1,.LBB1_2
; CHECK-LABEL:   # %bb.1: # %block1
; CHECK-NEXT:    SW a1,0(a2)
; CHECK-NEXT:    RET
; CHECK-NEXT:  .LBB1_2: # %block2
; CHECK-NEXT:    ADDI a0,zero,87
; CHECK-NEXT:    SW a0,0(a3)
; CHECK-NEXT:    RET
  store i32 -1, ptr %a
  %p = icmp sle i32 %b, -2
  br i1 %p, label %block1, label %block2

block1:                                           ; preds = %0
  store i32 %b, ptr %c
  br label %end_block

block2:                                           ; preds = %0
  store i32 87, ptr %d
  br label %end_block

end_block:                                        ; preds = %block2, %block1
  ret void
}

define void @u_case2_a(ptr %a, i32 signext %b, ptr %c, ptr %d) {
; CHECK-LABEL: u_case2_a:
; CHECK:       # %bb.0:
; CHECK-NEXT:    ADDI a4,zero,32
; CHECK-NEXT:    SW a4,0(a0)
; CHECK-NEXT:    ADDI a0,zero,33
; CHECK-NEXT:    BLTU a1,a0,.LBB2_2
; CHECK-NEXT:  # %bb.1: # %block1
; CHECK-NEXT:    SW a1,0(a2)
; CHECK-NEXT:    RET
; CHECK-NEXT:  .LBB2_2: # %block2
; CHECK-NEXT:    ADDI a0,zero,87
; CHECK-NEXT:    SW a0,0(a3)
; CHECK-NEXT:    RET
  store i32 32, ptr %a
  %p = icmp uge i32 %b, 33
  br i1 %p, label %block1, label %block2

block1:                                           ; preds = %0
  store i32 %b, ptr %c
  br label %end_block

block2:                                           ; preds = %0
  store i32 87, ptr %d
  br label %end_block

end_block:                                        ; preds = %block2, %block1
  ret void
}

define void @case2_a(ptr %a, i32 signext %b, ptr %c, ptr %d) {
; CHECK-LABEL: case2_a:
; CHECK:       # %bb.0:
; CHECK-LABEL: .Ltmp1:
; CHECK-NEXT: AUIPC a4,%pcrel_hi(.LCPI3_0)
; CHECK-NEXT: ADDI a4,a4,%pcrel_lo(.Ltmp1)
; CHECK-NEXT: LD a4,0(a4)
; CHECK-NEXT: SW a4,0(a0)
; CHECK-NEXT: ADDI a0,zero,-3
; CHECK-NEXT: BLT a1,a0,.LBB3_2
; CHECK-LABEL: # %bb.1: # %block1
; CHECK-NEXT: SW a1,0(a2)
; CHECK-NEXT: RET
; CHECK-LABEL: .LBB3_2: # %block2
; CHECK-NEXT: ADDI a0,zero,87
; CHECK-NEXT: SW a0,0(a3)
; CHECK-NEXT: RET
  store i32 -4, ptr %a
  %p = icmp sge i32 %b, -3
  br i1 %p, label %block1, label %block2

block1:                                           ; preds = %0
  store i32 %b, ptr %c
  br label %end_block

block2:                                           ; preds = %0
  store i32 87, ptr %d
  br label %end_block

end_block:                                        ; preds = %block2, %block1
  ret void
}