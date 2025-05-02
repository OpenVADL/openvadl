; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

@var = global [32 x i32] zeroinitializer

define void @callee() nounwind {
; CHECK-LABEL: callee:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-144
; CHECK-NEXT: SW ra,140(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s1,136(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s2,132(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s3,128(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s4,124(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s5,120(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s6,116(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s7,112(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s8,108(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s9,104(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s10,100(sp)                          # 4-byte Folded Spill
; CHECK-NEXT: SW s11,96(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: LUI s1,%hi(var)
; CHECK-NEXT: ADDI s1,s1,%lo(var)
; CHECK-NEXT: LW a0,0(s1)
; CHECK-NEXT: SW a0,92(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,4(s1)
; CHECK-NEXT: SW a0,88(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,8(s1)
; CHECK-NEXT: SW a0,84(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,12(s1)
; CHECK-NEXT: SW a0,80(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,16(s1)
; CHECK-NEXT: SW a0,76(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,20(s1)
; CHECK-NEXT: SW a0,72(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,24(s1)
; CHECK-NEXT: SW a0,68(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,28(s1)
; CHECK-NEXT: SW a0,64(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,32(s1)
; CHECK-NEXT: SW a0,60(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,36(s1)
; CHECK-NEXT: SW a0,56(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,40(s1)
; CHECK-NEXT: SW a0,52(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,44(s1)
; CHECK-NEXT: SW a0,48(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,48(s1)
; CHECK-NEXT: SW a0,44(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,52(s1)
; CHECK-NEXT: SW a0,40(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,56(s1)
; CHECK-NEXT: SW a0,36(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,60(s1)
; CHECK-NEXT: SW a0,32(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,64(s1)
; CHECK-NEXT: SW a0,28(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,68(s1)
; CHECK-NEXT: SW a0,24(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,72(s1)
; CHECK-NEXT: SW a0,20(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,76(s1)
; CHECK-NEXT: SW a0,16(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,80(s1)
; CHECK-NEXT: SW a0,12(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a0,84(s1)
; CHECK-NEXT: SW a0,8(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW s4,88(s1)
; CHECK-NEXT: LW s5,92(s1)
; CHECK-NEXT: LW s6,96(s1)
; CHECK-NEXT: LW s7,100(s1)
; CHECK-NEXT: LW s8,104(s1)
; CHECK-NEXT: LW s9,108(s1)
; CHECK-NEXT: LW s10,112(s1)
; CHECK-NEXT: LW s11,116(s1)
; CHECK-NEXT: LW s2,120(s1)
; CHECK-NEXT: LW s3,124(s1)
; CHECK-NEXT: CALL callee
; CHECK-NEXT: SW s3,124(s1)
; CHECK-NEXT: SW s2,120(s1)
; CHECK-NEXT: SW s11,116(s1)
; CHECK-NEXT: SW s10,112(s1)
; CHECK-NEXT: SW s9,108(s1)
; CHECK-NEXT: SW s8,104(s1)
; CHECK-NEXT: SW s7,100(s1)
; CHECK-NEXT: SW s6,96(s1)
; CHECK-NEXT: SW s5,92(s1)
; CHECK-NEXT: SW s4,88(s1)
; CHECK-NEXT: LW a0,8(sp)                             # 4-byte Folded Reload
; CHECK-NEXT: SW a0,84(s1)
; CHECK-NEXT: LW a0,12(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,80(s1)
; CHECK-NEXT: LW a0,16(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,76(s1)
; CHECK-NEXT: LW a0,20(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,72(s1)
; CHECK-NEXT: LW a0,24(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,68(s1)
; CHECK-NEXT: LW a0,28(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,64(s1)
; CHECK-NEXT: LW a0,32(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,60(s1)
; CHECK-NEXT: LW a0,36(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,56(s1)
; CHECK-NEXT: LW a0,40(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,52(s1)
; CHECK-NEXT: LW a0,44(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,48(s1)
; CHECK-NEXT: LW a0,48(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,44(s1)
; CHECK-NEXT: LW a0,52(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,40(s1)
; CHECK-NEXT: LW a0,56(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,36(s1)
; CHECK-NEXT: LW a0,60(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,32(s1)
; CHECK-NEXT: LW a0,64(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,28(s1)
; CHECK-NEXT: LW a0,68(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,24(s1)
; CHECK-NEXT: LW a0,72(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,20(s1)
; CHECK-NEXT: LW a0,76(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,16(s1)
; CHECK-NEXT: LW a0,80(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,12(s1)
; CHECK-NEXT: LW a0,84(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,8(s1)
; CHECK-NEXT: LW a0,88(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,4(s1)
; CHECK-NEXT: LW a0,92(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,0(s1)
; CHECK-NEXT: LW s11,96(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s10,100(sp)                          # 4-byte Folded Reload
; CHECK-NEXT: LW s9,104(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s8,108(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s7,112(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s6,116(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s5,120(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s4,124(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s3,128(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s2,132(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s1,136(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW ra,140(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,144
; CHECK-NEXT: RET
  %val = load [32 x i32], ptr @var
  call void @callee()
  store volatile [32 x i32] %val, ptr @var
  ret void
}