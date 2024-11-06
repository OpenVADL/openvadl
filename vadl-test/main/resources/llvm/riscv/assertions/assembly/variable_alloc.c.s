.text
.file	"variable_alloc.c"
.globl	compdecomp                      # -- Begin function compdecomp
.type	compdecomp,@function
compdecomp:                             # @compdecomp
# %bb.0:                                # %entry
ADDI X2,X2,2049
ADDI X2,X2,4063
SD X8,2072(X2)
ADDI X8,X2,2047
ADDI X8,X8,33
                                      # kill: def $x1 killed $x11
SD X10,4080(X8)
SW X11,4076(X8)
LD X8,2072(X2)
ADDI X2,X2,2047
ADDI X2,X2,33
JALR X0,0(X1)
.Lfunc_end0:
.size	compdecomp, .Lfunc_end0-compdecomp
                                      # -- End function
.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig