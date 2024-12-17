	.text
	.file	"variable_alloc.c"
	.globl	compdecomp                      # -- Begin function compdecomp
	.type	compdecomp,@function
compdecomp:                             # @compdecomp
# %bb.0:                                # %entry
	ADDI sp,sp,-2047
	ADDI sp,sp,-33
	ADDI ra,sp,2047
	ADDI ra,ra,25
	SD fp,0(ra)
	ADDI fp,sp,2047
	ADDI fp,fp,33
                                        # kill: def $x1 killed $x11
	SD a0,-16(fp)
	SW a1,-20(fp)
	ADDI ra,sp,2047
	ADDI ra,ra,25
	LD fp,0(ra)
	ADDI sp,sp,2047
	ADDI sp,sp,33
	JALR zero,0(ra)
.Lfunc_end0:
	.size	compdecomp, .Lfunc_end0-compdecomp
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
