.text
.file	"globals.c"
.globl	main                            # -- Begin function main
.type	main,@function
main:                                   # @main
# %bb.0:                                # %entry
ADDI X2,X2,4080
SD X8,8(X2)
ADDI X8,X2,16
ADDI X4,X0,0
SW X4,4084(X8)
LUI X1,%hi20(a)
ADDI X1,X1,%lo12(a)
SW X4,0(X1)
LW X1,0(X1)
SW X1,4080(X8)
LW X10,4080(X8)
LD X8,8(X2)
ADDI X2,X2,16
JALR X0,0(X1)
.Lfunc_end0:
.size	main, .Lfunc_end0-main
                                      # -- End function
.type	a,@object                       # @a
.bss
.globl	a
.p2align	2, 0x0
a:
.zero	4
.size	a, 4

.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig
.addrsig_sym a