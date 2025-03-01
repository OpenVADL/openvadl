; RUN: /src/llvm-final/build/bin/llvm-mc -arch=rv32im -show-inst < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT

SB x0, 10(x1)
; CHECK: <MCInst #{{[0-9]+}} SB
; CHECK-NEXT: <MCOperand Reg:3>
; CHECK-NEXT: <MCOperand Reg:2>
; CHECK-NEXT: <MCOperand Imm:10>>

SH x2, 15(x3)
; CHECK: <MCInst #{{[0-9]+}} SH
; CHECK-NEXT: <MCOperand Reg:5>
; CHECK-NEXT: <MCOperand Reg:4>
; CHECK-NEXT: <MCOperand Imm:15>>

SW x4, 20(x5)
; CHECK: <MCInst #{{[0-9]+}} SW
; CHECK-NEXT: <MCOperand Reg:7>
; CHECK-NEXT: <MCOperand Reg:6>
; CHECK-NEXT: <MCOperand Imm:20>>
