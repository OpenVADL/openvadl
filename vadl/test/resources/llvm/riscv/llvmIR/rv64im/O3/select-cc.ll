; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define signext i32 @foo(i32 signext %a, ptr %b) nounwind {
; CHECK-LABEL: foo:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: LUI a2,0x0
; CHECK-NEXT: ADDI a2,a2,0
; CHECK-NEXT: SLLI a2,a2,16
; CHECK-NEXT: ORI a2,a2,0
; CHECK-NEXT: SLLI a2,a2,16
; CHECK-NEXT: ORI a2,a2,-1
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BEQ a4,a3,.LBB0_1
; CHECK-NEXT: JAL zero,.LBB0_16
; CHECK-NEXT: .LBB0_1:
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BNE a4,a3,.LBB0_2
; CHECK-NEXT: JAL zero,.LBB0_17
; CHECK-NEXT: .LBB0_2:
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BLTU a3,a4,.LBB0_3
; CHECK-NEXT: JAL zero,.LBB0_18
; CHECK-NEXT: .LBB0_3:
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BGEU a4,a3,.LBB0_4
; CHECK-NEXT: JAL zero,.LBB0_19
; CHECK-NEXT: .LBB0_4:
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BLTU a4,a3,.LBB0_5
; CHECK-NEXT: JAL zero,.LBB0_20
; CHECK-NEXT: .LBB0_5:
; CHECK-NEXT: AND a3,a0,a2
; CHECK-NEXT: LWU a2,0(a1)
; CHECK-NEXT: BGEU a2,a3,.LBB0_6
; CHECK-NEXT: JAL zero,.LBB0_21
; CHECK-NEXT: .LBB0_6:
; CHECK-NEXT: SLLI a2,a0,32
; CHECK-NEXT: SRAI a3,a2,32
; CHECK-NEXT: LW a2,0(a1)
; CHECK-NEXT: BLT a2,a3,.LBB0_7
; CHECK-NEXT: JAL zero,.LBB0_22
; CHECK-NEXT: .LBB0_7:
; CHECK-NEXT: SLLI a2,a0,32
; CHECK-NEXT: SRAI a3,a2,32
; CHECK-NEXT: LW a2,0(a1)
; CHECK-NEXT: BGE a3,a2,.LBB0_8
; CHECK-NEXT: JAL zero,.LBB0_23
; CHECK-NEXT: .LBB0_8:
; CHECK-NEXT: SLLI a2,a0,32
; CHECK-NEXT: SRAI a3,a2,32
; CHECK-NEXT: LW a2,0(a1)
; CHECK-NEXT: BLT a3,a2,.LBB0_9
; CHECK-NEXT: JAL zero,.LBB0_24
; CHECK-NEXT: .LBB0_9:
; CHECK-NEXT: SLLI a2,a0,32
; CHECK-NEXT: SRAI a3,a2,32
; CHECK-NEXT: LW a2,0(a1)
; CHECK-NEXT: BGE a2,a3,.LBB0_11
; CHECK-NEXT: .LBB0_10:
; CHECK-NEXT: ADDI a0,a2,0
; CHECK-NEXT: .LBB0_11:
; CHECK-NEXT: LWU a2,0(a1)
; CHECK-NEXT: SLLI a3,a2,32
; CHECK-NEXT: SRAI a3,a3,32
; CHECK-NEXT: ADDI a4,zero,1
; CHECK-NEXT: BLT a3,a4,.LBB0_12
; CHECK-NEXT: JAL zero,.LBB0_25
; CHECK-NEXT: .LBB0_12:
; CHECK-NEXT: ADDI a5,zero,-1
; CHECK-NEXT: LW a4,0(a1)
; CHECK-NEXT: BLT a5,a3,.LBB0_13
; CHECK-NEXT: JAL zero,.LBB0_26
; CHECK-NEXT: .LBB0_13:
; CHECK-NEXT: ADDI a4,zero,1024
; CHECK-NEXT: LW a3,0(a1)
; CHECK-NEXT: BLT a4,a3,.LBB0_14
; CHECK-NEXT: JAL zero,.LBB0_27
; CHECK-NEXT: .LBB0_14:
; CHECK-NEXT: LW a1,0(a1)
; CHECK-NEXT: ADDI a3,zero,2046
; CHECK-NEXT: BLTU a3,a2,.LBB0_15
; CHECK-NEXT: JAL zero,.LBB0_28
; CHECK-NEXT: .LBB0_15:
; CHECK-NEXT: SLLI a0,a0,32
; CHECK-NEXT: SRAI a0,a0,32
; CHECK-NEXT: RET
; CHECK-NEXT: .LBB0_16:
; CHECK-NEXT: ADDI a0,a3,0
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BNE a4,a3,.LBB0_2
; CHECK-NEXT: .LBB0_17:
; CHECK-NEXT: ADDI a0,a3,0
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BLTU a3,a4,.LBB0_3
; CHECK-NEXT: .LBB0_18:
; CHECK-NEXT: ADDI a0,a3,0
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BGEU a4,a3,.LBB0_4
; CHECK-NEXT: .LBB0_19:
; CHECK-NEXT: ADDI a0,a3,0
; CHECK-NEXT: AND a4,a0,a2
; CHECK-NEXT: LWU a3,0(a1)
; CHECK-NEXT: BLTU a4,a3,.LBB0_5
; CHECK-NEXT: .LBB0_20:
; CHECK-NEXT: ADDI a0,a3,0
; CHECK-NEXT: AND a3,a0,a2
; CHECK-NEXT: LWU a2,0(a1)
; CHECK-NEXT: BGEU a2,a3,.LBB0_6
; CHECK-NEXT: .LBB0_21:
; CHECK-NEXT: ADDI a0,a2,0
; CHECK-NEXT: SLLI a2,a0,32
; CHECK-NEXT: SRAI a3,a2,32
; CHECK-NEXT: LW a2,0(a1)
; CHECK-NEXT: BLT a2,a3,.LBB0_7
; CHECK-NEXT: .LBB0_22:
; CHECK-NEXT: ADDI a0,a2,0
; CHECK-NEXT: SLLI a2,a0,32
; CHECK-NEXT: SRAI a3,a2,32
; CHECK-NEXT: LW a2,0(a1)
; CHECK-NEXT: BGE a3,a2,.LBB0_8
; CHECK-NEXT: .LBB0_23:
; CHECK-NEXT: ADDI a0,a2,0
; CHECK-NEXT: SLLI a2,a0,32
; CHECK-NEXT: SRAI a3,a2,32
; CHECK-NEXT: LW a2,0(a1)
; CHECK-NEXT: BLT a3,a2,.LBB0_9
; CHECK-NEXT: .LBB0_24:
; CHECK-NEXT: ADDI a0,a2,0
; CHECK-NEXT: SLLI a2,a0,32
; CHECK-NEXT: SRAI a3,a2,32
; CHECK-NEXT: LW a2,0(a1)
; CHECK-NEXT: BGE a2,a3,.LBB0_11
; CHECK-NEXT: JAL zero,.LBB0_10
; CHECK-NEXT: .LBB0_25:
; CHECK-NEXT: ADDI a0,a2,0
; CHECK-NEXT: ADDI a5,zero,-1
; CHECK-NEXT: LW a4,0(a1)
; CHECK-NEXT: BLT a5,a3,.LBB0_13
; CHECK-NEXT: .LBB0_26:
; CHECK-NEXT: ADDI a0,a4,0
; CHECK-NEXT: ADDI a4,zero,1024
; CHECK-NEXT: LW a3,0(a1)
; CHECK-NEXT: BLT a4,a3,.LBB0_14
; CHECK-NEXT: .LBB0_27:
; CHECK-NEXT: ADDI a0,a3,0
; CHECK-NEXT: LW a1,0(a1)
; CHECK-NEXT: ADDI a3,zero,2046
; CHECK-NEXT: BLTU a3,a2,.LBB0_15
; CHECK-NEXT: .LBB0_28:
; CHECK-NEXT: ADDI a0,a1,0
; CHECK-NEXT: SLLI a0,a0,32
; CHECK-NEXT: SRAI a0,a0,32
; CHECK-NEXT: RET
  %val1 = load volatile i32, ptr %b
  %tst1 = icmp eq i32 %a, %val1
  %val2 = select i1 %tst1, i32 %a, i32 %val1

  %val3 = load volatile i32, ptr %b
  %tst2 = icmp ne i32 %val2, %val3
  %val4 = select i1 %tst2, i32 %val2, i32 %val3

  %val5 = load volatile i32, ptr %b
  %tst3 = icmp ugt i32 %val4, %val5
  %val6 = select i1 %tst3, i32 %val4, i32 %val5

  %val7 = load volatile i32, ptr %b
  %tst4 = icmp uge i32 %val6, %val7
  %val8 = select i1 %tst4, i32 %val6, i32 %val7

  %val9 = load volatile i32, ptr %b
  %tst5 = icmp ult i32 %val8, %val9
  %val10 = select i1 %tst5, i32 %val8, i32 %val9

  %val11 = load volatile i32, ptr %b
  %tst6 = icmp ule i32 %val10, %val11
  %val12 = select i1 %tst6, i32 %val10, i32 %val11

  %val13 = load volatile i32, ptr %b
  %tst7 = icmp sgt i32 %val12, %val13
  %val14 = select i1 %tst7, i32 %val12, i32 %val13

  %val15 = load volatile i32, ptr %b
  %tst8 = icmp sge i32 %val14, %val15
  %val16 = select i1 %tst8, i32 %val14, i32 %val15

  %val17 = load volatile i32, ptr %b
  %tst9 = icmp slt i32 %val16, %val17
  %val18 = select i1 %tst9, i32 %val16, i32 %val17

  %val19 = load volatile i32, ptr %b
  %tst10 = icmp sle i32 %val18, %val19
  %val20 = select i1 %tst10, i32 %val18, i32 %val19

  %val21 = load volatile i32, ptr %b
  %tst11 = icmp slt i32 %val21, 1
  %val22 = select i1 %tst11, i32 %val20, i32 %val21

  %val23 = load volatile i32, ptr %b
  %tst12 = icmp sgt i32 %val21, -1
  %val24 = select i1 %tst12, i32 %val22, i32 %val23

  %val25 = load volatile i32, ptr %b
  %tst13 = icmp sgt i32 %val25, 1024
  %val26 = select i1 %tst13, i32 %val24, i32 %val25

  %val27 = load volatile i32, ptr %b
  %tst14 = icmp ugt i32 %val21, 2046
  %val28 = select i1 %tst14, i32 %val26, i32 %val27
  ret i32 %val28
}

declare void @bar(i16 signext)

define i32 @select_sge_int16min(i32 signext %x, i32 signext %y, i32 signext %z) {
; CHECK-LABEL: select_sge_int16min:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: LUI a2,0xffff0
; CHECK-NEXT: ADDI a2,a2,-1
; CHECK-NEXT: BLT a2,a0,.LBB1_2
; CHECK-NEXT: # %bb.1:
; CHECK-NEXT: LD a1,0(sp)
; CHECK-NEXT: .LBB1_2:
; CHECK-NEXT: ADDI a0,a1,0
; CHECK-NEXT: RET
  %a = icmp sge i32 %x, -65536
  %b = select i1 %a, i32 %y, i32 %z
  ret i32 %b
}

define i64 @select_sge_int32min(i64 %x, i64 %y, i64 %z) {
; CHECK-LABEL: select_sge_int32min:
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: LUI a2,0xfffff
; CHECK-NEXT: ADDI a2,a2,-1
; CHECK-NEXT: SLLI a2,a2,16
; CHECK-NEXT: ORI a2,a2,-1
; CHECK-NEXT: SLLI a2,a2,16
; CHECK-NEXT: ORI a2,a2,-1
; CHECK-NEXT: BLT a2,a0,.LBB2_2
; CHECK-LABEL: # %bb.1:
; CHECK-NEXT: LD a1,0(sp)
; CHECK-LABEL: .LBB2_2:
; CHECK-NEXT: ADDI a0,a1,0
; CHECK-NEXT: RET
  %a = icmp sge i64 %x, -2147483648
  %b = select i1 %a, i64 %y, i64 %z
  ret i64 %b
}