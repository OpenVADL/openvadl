; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

@var = global [32 x i32] zeroinitializer

define void @callee() nounwind {
; CHECK-LABEL: callee:
; CHECK: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-272
; CHECK-NEXT: SD ra,264(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s1,256(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s2,248(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s3,240(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s4,232(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s5,224(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s6,216(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s7,208(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s8,200(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s9,192(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: SD s10,184(sp)                          # 8-byte Folded Spill
; CHECK-NEXT: SD s11,176(sp)                          # 8-byte Folded Spill
; CHECK-NEXT: LUI s1,%hi(var)
; CHECK-NEXT: ADDI s1,s1,%lo(var)
; CHECK-NEXT: ADDI a0,s1,4
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,168(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,8
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,160(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,12
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,152(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,16
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,144(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,20
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,136(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,24
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,128(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,28
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,120(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,32
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,112(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,36
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,104(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,40
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,96(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,44
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,88(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,48
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,80(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,52
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,72(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,56
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,64(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,60
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,56(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,64
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,48(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,68
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,40(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,72
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,32(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,76
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,24(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,80
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,16(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,84
; CHECK-NEXT: LW a0,0(a0)
; CHECK-NEXT: SD a0,8(sp)                           # 8-byte Folded Spill
; CHECK-NEXT: ADDI a0,s1,88
; CHECK-NEXT: LW s3,0(a0)
; CHECK-NEXT: ADDI a0,s1,92
; CHECK-NEXT: LW s4,0(a0)
; CHECK-NEXT: ADDI a0,s1,96
; CHECK-NEXT: LW s5,0(a0)
; CHECK-NEXT: ADDI a0,s1,100
; CHECK-NEXT: LW s6,0(a0)
; CHECK-NEXT: ADDI a0,s1,104
; CHECK-NEXT: LW s7,0(a0)
; CHECK-NEXT: ADDI a0,s1,108
; CHECK-NEXT: LW s8,0(a0)
; CHECK-NEXT: ADDI a0,s1,112
; CHECK-NEXT: LW s9,0(a0)
; CHECK-NEXT: ADDI a0,s1,116
; CHECK-NEXT: LW s10,0(a0)
; CHECK-NEXT: ADDI a0,s1,120
; CHECK-NEXT: LW s11,0(a0)
; CHECK-NEXT: ADDI a0,s1,124
; CHECK-NEXT: LW s2,0(a0)
; CHECK-NEXT: LW a0,0(s1)
; CHECK-NEXT: SD a0,0(sp)                             # 8-byte Folded Spill
; CHECK-NEXT: CALL callee
; CHECK-NEXT: SW s2,124(s1)
; CHECK-NEXT: SW s11,120(s1)
; CHECK-NEXT: SW s10,116(s1)
; CHECK-NEXT: SW s9,112(s1)
; CHECK-NEXT: SW s8,108(s1)
; CHECK-NEXT: SW s7,104(s1)
; CHECK-NEXT: SW s6,100(s1)
; CHECK-NEXT: SW s5,96(s1)
; CHECK-NEXT: SW s4,92(s1)
; CHECK-NEXT: SW s3,88(s1)
; CHECK-NEXT: LD a0,8(sp)                             # 8-byte Folded Reload
; CHECK-NEXT: SW a0,84(s1)
; CHECK-NEXT: LD a0,16(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,80(s1)
; CHECK-NEXT: LD a0,24(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,76(s1)
; CHECK-NEXT: LD a0,32(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,72(s1)
; CHECK-NEXT: LD a0,40(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,68(s1)
; CHECK-NEXT: LD a0,48(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,64(s1)
; CHECK-NEXT: LD a0,56(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,60(s1)
; CHECK-NEXT: LD a0,64(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,56(s1)
; CHECK-NEXT: LD a0,72(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,52(s1)
; CHECK-NEXT: LD a0,80(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,48(s1)
; CHECK-NEXT: LD a0,88(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,44(s1)
; CHECK-NEXT: LD a0,96(sp)                            # 8-byte Folded Reload
; CHECK-NEXT: SW a0,40(s1)
; CHECK-NEXT: LD a0,104(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,36(s1)
; CHECK-NEXT: LD a0,112(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,32(s1)
; CHECK-NEXT: LD a0,120(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,28(s1)
; CHECK-NEXT: LD a0,128(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,24(s1)
; CHECK-NEXT: LD a0,136(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,20(s1)
; CHECK-NEXT: LD a0,144(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,16(s1)
; CHECK-NEXT: LD a0,152(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,12(s1)
; CHECK-NEXT: LD a0,160(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,8(s1)
; CHECK-NEXT: LD a0,168(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: SW a0,4(s1)
; CHECK-NEXT: LD a0,0(sp)                             # 8-byte Folded Reload
; CHECK-NEXT: SW a0,0(s1)
; CHECK-NEXT: LD s11,176(sp)                          # 8-byte Folded Reload
; CHECK-NEXT: LD s10,184(sp)                          # 8-byte Folded Reload
; CHECK-NEXT: LD s9,192(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD s8,200(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD s7,208(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD s6,216(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD s5,224(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD s4,232(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD s3,240(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD s2,248(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD s1,256(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: LD ra,264(sp)                           # 8-byte Folded Reload
; CHECK-NEXT: ADDI sp,sp,272
; CHECK-NEXT: RET
  %val = load [32 x i32], ptr @var
  call void @callee()
  store volatile [32 x i32] %val, ptr @var
  ret void
}