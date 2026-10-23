; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

; Keep wide scalars in the IR signature so this exercises backend argument
; lowering independently of Clang's indirect parameter classification.
define void @multiply_quad(ptr %result, fp128 %lhs, fp128 %rhs) {
; CHECK-LABEL: multiply_quad:
; The incoming quad-precision operands are pointers in a1 and a2.
; CHECK-DAG: LW {{[a-z][a-z0-9]*}},0(a1)
; CHECK-DAG: LW {{[a-z][a-z0-9]*}},0(a2)
; The libgcc helper also receives pointers to both operands.
; CHECK-DAG: ADDI a1,sp,{{[0-9]+}}
; CHECK-DAG: ADDI a2,sp,{{[0-9]+}}
; CHECK: CALL __multf3
; CHECK: RET
  %product = fmul fp128 %lhs, %rhs
  store fp128 %product, ptr %result, align 16
  ret void
}
