; RUN: FileCheck %s
define i32 @and32_0x7ff(i32 %x) {
; CHECK-LABEL: and32_0x7ff: # @and32_0x7ff
; CHECK:         ADDI a1,zero,2047
; CHECK-NEXT:    AND a0,a0,a1
; CHECK-NEXT:    JALR zero,0(ra)
  %a = and i32 %x, 2047
  ret i32 %a
}