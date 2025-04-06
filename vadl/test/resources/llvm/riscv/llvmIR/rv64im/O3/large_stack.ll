; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define void @smalltest() {
; CHECK-LABEL: smalltest: # @smalltest
; CHECK: ADDI sp,sp,-1024
; CHECK-NEXT: ADDI sp,sp,1024
  %tmp = alloca [ 1024 x i8 ] , align 4
  ret void
}

define void @middletest() {
; CHECK-LABEL: middletest: # @middletest
; CHECK: LUI a0,0xfffff
; CHECK-NEXT: ADDI a0,a0,48
; CHECK-NEXT: ADD sp,sp,a0
; CHECK-NEXT: LUI a0,0x1
; CHECK-NEXT: ADDI a0,a0,-48
; CHECK-NEXT: ADD sp,sp,a0
  %tmp = alloca [ 4048 x i8 ] , align 4
  ret void
}

define void @test() {
; CHECK-LABEL: test: # @test
; CHECK: LUI a0,0xedcbb
; CHECK-NEXT: ADDI a0,a0,-1664
; CHECK-NEXT: ADD sp,sp,a0
; CHECK-NEXT: LUI a0,0x12345
; CHECK-NEXT: ADDI a0,a0,1664
; CHECK-NEXT: ADD sp,sp,a0
  %tmp = alloca [ 305419896 x i8 ] , align 4
  ret void
}

; This test case artificially produces register pressure which should force
; use of the emergency spill slot.

define void @test_emergency_spill_slot(i32 %a) {
; CHECK-LABEL: test_emergency_spill_slot: # @test_emergency_spill_slot
; CHECK: LUI a1,0xfff9e
; CHECK-NEXT: ADDI a1,a1,1408
; CHECK-NEXT: ADD sp,sp,a1
; CHECK-NEXT: LUI a1,0x4e
; CHECK-NEXT: ADDI a1,a1,512
; CHECK-NEXT: ADDI a2,sp,0
; CHECK-NEXT: ADD a1,a2,a1
; CHECK-NEXT: SW a0,0(a1)
; CHECK-NEXT: LUI a0,0x62
; CHECK-NEXT: ADDI a0,a0,-1408
; CHECK-NEXT: ADD sp,sp,a0
; CHECK-NEXT: JALR zero,0(ra)
  %data = alloca [ 100000 x i32 ] , align 4
  %ptr = getelementptr inbounds [100000 x i32], ptr %data, i32 0, i32 80000
  %1 = tail call { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } asm sideeffect "nop", "=r,=r,=r,=r,=r,=r,=r,=r,=r,=r,=r,=r,=r,=r,=r"()
  %asmresult0 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 0
  %asmresult1 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 1
  %asmresult2 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 2
  %asmresult3 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 3
  %asmresult4 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 4
  %asmresult5 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 5
  %asmresult6 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 6
  %asmresult7 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 7
  %asmresult8 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 8
  %asmresult9 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 9
  %asmresult10 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 10
  %asmresult11 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 11
  %asmresult12 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 12
  %asmresult13 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 13
  %asmresult14 = extractvalue { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 } %1, 14
  store volatile i32 %a, ptr %ptr
  tail call void asm sideeffect "nop", "r,r,r,r,r,r,r,r,r,r,r,r,r,r,r"(i32 %asmresult0, i32 %asmresult1, i32 %asmresult2, i32 %asmresult3, i32 %asmresult4, i32 %asmresult5, i32 %asmresult6, i32 %asmresult7, i32 %asmresult8, i32 %asmresult9, i32 %asmresult10, i32 %asmresult11, i32 %asmresult12, i32 %asmresult13, i32 %asmresult14)
  ret void
}