; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

@var = global [32 x i32] zeroinitializer

define void @callee() nounwind {
; CHECK-LABEL: callee:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-80
; CHECK-NEXT: SW ra,76(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s1,72(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s2,68(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s3,64(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s4,60(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s5,56(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s6,52(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s7,48(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s8,44(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s9,40(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: SW s10,36(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: SW s11,32(sp)                           # 4-byte Folded Spill
; CHECK-NEXT: LUI a0,%hi(var)
; CHECK-NEXT: ADDI a0,a0,%lo(var)
; CHECK-NEXT: LW a1,0(a0)
; CHECK-NEXT: SW a1,28(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a2,4(a0)
; CHECK-NEXT: LW a3,8(a0)
; CHECK-NEXT: LW a4,12(a0)
; CHECK-NEXT: LW a5,16(a0)
; CHECK-NEXT: LW a6,20(a0)
; CHECK-NEXT: LW a7,24(a0)
; CHECK-NEXT: LW t0,28(a0)
; CHECK-NEXT: LW t1,32(a0)
; CHECK-NEXT: LW t2,36(a0)
; CHECK-NEXT: LW t3,40(a0)
; CHECK-NEXT: LW t4,44(a0)
; CHECK-NEXT: LW t5,48(a0)
; CHECK-NEXT: LW t6,52(a0)
; CHECK-NEXT: LW s1,56(a0)
; CHECK-NEXT: LW s2,60(a0)
; CHECK-NEXT: LW s3,64(a0)
; CHECK-NEXT: LW s4,68(a0)
; CHECK-NEXT: LW s5,72(a0)
; CHECK-NEXT: LW s6,76(a0)
; CHECK-NEXT: LW s7,80(a0)
; CHECK-NEXT: LW s8,84(a0)
; CHECK-NEXT: LW s9,88(a0)
; CHECK-NEXT: LW s10,92(a0)
; CHECK-NEXT: LW s11,96(a0)
; CHECK-NEXT: LW ra,100(a0)
; CHECK-NEXT: LW a1,104(a0)
; CHECK-NEXT: SW a1,24(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a1,108(a0)
; CHECK-NEXT: SW a1,20(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a1,112(a0)
; CHECK-NEXT: SW a1,16(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a1,116(a0)
; CHECK-NEXT: SW a1,12(sp)                            # 4-byte Folded Spill
; CHECK-NEXT: LW a1,120(a0)
; CHECK-NEXT: SW a1,8(sp)                             # 4-byte Folded Spill
; CHECK-NEXT: LW a1,124(a0)
; CHECK-NEXT: SW a1,124(a0)
; CHECK-NEXT: LW a1,8(sp)                             # 4-byte Folded Reload
; CHECK-NEXT: SW a1,120(a0)
; CHECK-NEXT: LW a1,12(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a1,116(a0)
; CHECK-NEXT: LW a1,16(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a1,112(a0)
; CHECK-NEXT: LW a1,20(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a1,108(a0)
; CHECK-NEXT: LW a1,24(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a1,104(a0)
; CHECK-NEXT: LW a1,28(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW ra,100(a0)
; CHECK-NEXT: SW s11,96(a0)
; CHECK-NEXT: SW s10,92(a0)
; CHECK-NEXT: SW s9,88(a0)
; CHECK-NEXT: SW s8,84(a0)
; CHECK-NEXT: SW s7,80(a0)
; CHECK-NEXT: SW s6,76(a0)
; CHECK-NEXT: SW s5,72(a0)
; CHECK-NEXT: SW s4,68(a0)
; CHECK-NEXT: SW s3,64(a0)
; CHECK-NEXT: SW s2,60(a0)
; CHECK-NEXT: SW s1,56(a0)
; CHECK-NEXT: SW t6,52(a0)
; CHECK-NEXT: SW t5,48(a0)
; CHECK-NEXT: SW t4,44(a0)
; CHECK-NEXT: SW t3,40(a0)
; CHECK-NEXT: SW t2,36(a0)
; CHECK-NEXT: SW t1,32(a0)
; CHECK-NEXT: SW t0,28(a0)
; CHECK-NEXT: SW a7,24(a0)
; CHECK-NEXT: SW a6,20(a0)
; CHECK-NEXT: SW a5,16(a0)
; CHECK-NEXT: SW a4,12(a0)
; CHECK-NEXT: SW a3,8(a0)
; CHECK-NEXT: SW a2,4(a0)
; CHECK-NEXT: SW a1,0(a0)
; CHECK-NEXT: LW s11,32(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s10,36(sp)                           # 4-byte Folded Reload
; CHECK-NEXT: LW s9,40(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW s8,44(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW s7,48(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW s6,52(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW s5,56(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW s4,60(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW s3,64(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW s2,68(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW s1,72(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: LW ra,76(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,80
; CHECK-NEXT: JALR zero,0(ra)
  %val = load [32 x i32], ptr @var
  store volatile [32 x i32] %val, ptr @var
  ret void
}

define void @caller() nounwind {
; CHECK-LABEL: caller:
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
; CHECK-NEXT: LW ra,84(s1)
; CHECK-NEXT: LW s2,88(s1)
; CHECK-NEXT: LW s3,92(s1)
; CHECK-NEXT: LW s4,96(s1)
; CHECK-NEXT: LW s5,100(s1)
; CHECK-NEXT: LW s6,104(s1)
; CHECK-NEXT: LW s7,108(s1)
; CHECK-NEXT: LW s8,112(s1)
; CHECK-NEXT: LW s9,116(s1)
; CHECK-NEXT: LW s10,120(s1)
; CHECK-NEXT: LW s11,124(s1)
; CHECK-NEXT: LUI ra,%hi(callee)
; CHECK-NEXT: JALR ra,%lo(callee)(ra)
; CHECK-NEXT: SW s11,124(s1)
; CHECK-NEXT: SW s10,120(s1)
; CHECK-NEXT: SW s9,116(s1)
; CHECK-NEXT: SW s8,112(s1)
; CHECK-NEXT: SW s7,108(s1)
; CHECK-NEXT: SW s6,104(s1)
; CHECK-NEXT: SW s5,100(s1)
; CHECK-NEXT: SW s4,96(s1)
; CHECK-NEXT: SW s3,92(s1)
; CHECK-NEXT: SW s2,88(s1)
; CHECK-NEXT: SW ra,84(s1)
; CHECK-NEXT: LW a0,12(sp)                            # 4-byte Folded Reload
; CHECK-NEXT: SW a0,80(s1)
; CHECK-NEXT: LW a0,16(sp)                            # 4-byte Folded Reload
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
; CHECK-NEXT: JALR zero,0(ra)
  %val = load [32 x i32], ptr @var
  call void @callee()
  store volatile [32 x i32] %val, ptr @var
  ret void
}