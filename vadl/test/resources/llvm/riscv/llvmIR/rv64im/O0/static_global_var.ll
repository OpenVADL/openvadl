; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im --relocation-model=pic -O0 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT
@external_var = external global i32
@internal_var = internal global i32 42

@heap_ptr = internal global ptr null, align 8
@heap_end = internal global ptr null, align 8
@heap_requested = internal global i64 0, align 8
define void @init_heap_beebs(ptr noundef %heap, i64 noundef %heap_size) #0 {
; CHECK-LABEL: init_heap_beebs: # @init_heap_beebs
; CHECK-NEXT: # %bb.0:
; CHECK-NEXT: ADDI sp,sp,-16
; CHECK-NEXT: SD a0,8(sp)
; CHECK-NEXT: SD a1,0(sp)
; CHECK-NEXT: LD a1,8(sp)
; CHECK-LABEL: .Ltmp0:
; CHECK-NEXT: AUIPC a0,%pcrel_hi(heap_ptr)
; CHECK-NEXT: ADDI a0,a0,%pcrel_lo(.Ltmp0)
; CHECK-NEXT: SD a1,0(a0)
; CHECK-NEXT: LD a0,0(a0)
; CHECK-NEXT: LD a1,0(sp)
; CHECK-NEXT: ADD a1,a0,a1
; CHECK-LABEL: .Ltmp1:
; CHECK-NEXT: AUIPC a0,%pcrel_hi(heap_end)
; CHECK-NEXT: ADDI a0,a0,%pcrel_lo(.Ltmp1)
; CHECK-NEXT: SD a1,0(a0)
; CHECK-LABEL: .Ltmp2:
; CHECK-NEXT: AUIPC a0,%pcrel_hi(heap_requested)
; CHECK-NEXT: ADDI a0,a0,%pcrel_lo(.Ltmp2)
; CHECK-NEXT: ADDI a1,zero,0
; CHECK-NEXT: SD a1,0(a0)
; CHECK-NEXT: ADDI sp,sp,16
; CHECK-NEXT: RET
entry:
  %heap.addr = alloca ptr, align 8
  %heap_size.addr = alloca i64, align 8
  store ptr %heap, ptr %heap.addr, align 8
  store i64 %heap_size, ptr %heap_size.addr, align 8
  %0 = load ptr, ptr %heap.addr, align 8
  store ptr %0, ptr @heap_ptr, align 8
  %1 = load ptr, ptr @heap_ptr, align 8
  %2 = load i64, ptr %heap_size.addr, align 8
  %add.ptr = getelementptr inbounds i8, ptr %1, i64 %2
  store ptr %add.ptr, ptr @heap_end, align 8
  store i64 0, ptr @heap_requested, align 8
  ret void
}