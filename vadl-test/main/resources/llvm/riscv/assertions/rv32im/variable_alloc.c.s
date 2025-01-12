	.text
	.file	"variable_alloc.c"
	.globl	compdecomp                      # -- Begin function compdecomp
	.type	compdecomp,@function
compdecomp:                             # @compdecomp
# %bb.0:                                # %entry
	ADDI sp,sp,-2047
	ADDI sp,sp,-17
	ADDI a2,sp,2047
	ADDI a2,a2,13
	SW fp,0(a2)
	ADDI a2,sp,2047
	ADDI a2,a2,9
	SW ra,0(a2)
	ADDI fp,sp,2047
	ADDI fp,fp,17
	SW a0,-12(fp)
	SW a1,-16(fp)
	ADDI a0,sp,2047
	ADDI a0,a0,9
	LW ra,0(a0)
	ADDI a0,sp,2047
	ADDI a0,a0,13
	LW fp,0(a0)
	ADDI sp,sp,2047
	ADDI sp,sp,17
	JALR zero,0(ra)
.Lfunc_end0:
	.size	compdecomp, .Lfunc_end0-compdecomp
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
