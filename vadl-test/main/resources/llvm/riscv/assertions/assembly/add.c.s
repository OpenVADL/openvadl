.text
.file	"add.c"
.globl	constant_return                 # -- Begin function constant_return
.type	constant_return,@function
constant_return:                        # @constant_return
# %bb.0:                                # %entry
ADDI X2,X2,4080
SD X8,8(X2)
ADDI X8,X2,16
                                      # kill: def $x1 killed $x11
                                      # kill: def $x1 killed $x10
SW X10,4084(X8)
SW X11,4080(X8)
LW X1,4084(X8)
LW X4,4080(X8)
ADD X10,X1,X4
LD X8,8(X2)
ADDI X2,X2,16
JALR X0,0(X1)
.Lfunc_end0:
.size	constant_return, .Lfunc_end0-constant_return
                                      # -- End function
.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig