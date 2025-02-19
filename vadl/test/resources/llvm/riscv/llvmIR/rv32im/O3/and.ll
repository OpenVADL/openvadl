; RUN: /src/llvm-final/build/bin/llc -mtriple=rv32im -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

define i32 @and32_0x7ff(i32 %x) {
; CHECK-LABEL: and32_0x7ff: # @and32_0x7ff
; CHECK:         ANDI a0,a0,2047
; CHECK-NEXT:    JALR zero,0(ra)
  %a = and i32 %x, 2047
  ret i32 %a
}

define i32 @and32_0xfff(i32 %x) {
; CHECK-LABEL: and32_0xfff: # @and32_0xfff
; CHECK:       LUI a1,0x1
; CHECK-NEXT:  ADDI a1,a1,-1
; CHECK-NEXT:  AND a0,a0,a1
; CHECK-NEXT:  JALR zero,0(ra)
  %a = and i32 %x, 4095
  ret i32 %a
}

define i64 @and64_0x7ff(i64 %x) {
; CHECK-LABEL: and64_0x7ff: # @and64_0x7ff
; CHECK:         ANDI a0,a0,2047
; CHECK-NEXT:    ADDI a1,zero,0
; CHECK-NEXT:    JALR zero,0(ra)
  %a = and i64 %x, 2047
  ret i64 %a
}

define i64 @and64_0xfff(i64 %x) {
; CHECK-LABEL: and64_0xfff: # @and64_0xfff
; CHECK:       LUI a1,0x1
; CHECK-NEXT:  ADDI a1,a1,-1
; CHECK-NEXT:  AND a0,a0,a1
; CHECK-NEXT:  ADDI a1,zero,0
; CHECK-NEXT:  JALR zero,0(ra)
  %a = and i64 %x, 4095
  ret i64 %a
}