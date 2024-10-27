.text
.file	"arg_spilling.c"
.globl	arg_spilling                    # -- Begin function arg_spilling
.type	arg_spilling,@function
arg_spilling:                           # @arg_spilling
# %bb.0:                                # %entry
ADDI X2,X2,4048
SD X8,40(X2)
ADDI X8,X2,48
LD X1,0(X8)
                                      # kill: def $x4 killed $x17
                                      # kill: def $x4 killed $x16
                                      # kill: def $x4 killed $x15
                                      # kill: def $x4 killed $x14
                                      # kill: def $x4 killed $x13
                                      # kill: def $x4 killed $x12
                                      # kill: def $x4 killed $x11
                                      # kill: def $x4 killed $x10
SW X10,4084(X8)
SW X11,4080(X8)
SW X12,4076(X8)
SW X13,4072(X8)
SW X14,4068(X8)
SW X15,4064(X8)
SW X16,4060(X8)
SW X17,4056(X8)
SW X1,4052(X8)
LW X1,4084(X8)
LW X4,4080(X8)
ADD X1,X1,X4
LW X4,4076(X8)
ADD X1,X1,X4
LW X4,4072(X8)
ADD X1,X1,X4
LW X4,4068(X8)
ADD X1,X1,X4
LW X4,4064(X8)
ADD X1,X1,X4
LW X4,4060(X8)
ADD X1,X1,X4
LW X4,4056(X8)
ADD X1,X1,X4
LW X4,4052(X8)
ADD X10,X1,X4
LD X8,40(X2)
ADDI X2,X2,48
JALR X0,0(X1)
.Lfunc_end0:
.size	arg_spilling, .Lfunc_end0-arg_spilling
                                      # -- End function
.globl	arg_spilling_call               # -- Begin function arg_spilling_call
.type	arg_spilling_call,@function
arg_spilling_call:                      # @arg_spilling_call
# %bb.0:                                # %entry
ADDI X2,X2,4064
SD X2,24(X2)
SD X8,16(X2)
ADDI X8,X2,32
ADDI X1,X0,9
SD X1,0(X2)
ADDI X10,X0,1
ADDI X11,X0,2
ADDI X12,X0,3
ADDI X13,X0,4
ADDI X14,X0,5
ADDI X15,X0,6
ADDI X16,X0,7
ADDI X17,X0,8
LUI X1,%hi20(arg_spilling)
JALR X1,%lo12(arg_spilling)(X1)
LD X8,16(X2)
LD X2,24(X2)
ADDI X2,X2,32
JALR X0,0(X1)
.Lfunc_end1:
.size	arg_spilling_call, .Lfunc_end1-arg_spilling_call
                                      # -- End function
.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig
.addrsig_sym arg_spilling