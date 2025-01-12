	.text
	.file	"variable_alloc.c"
	.globl	compdecomp                      # -- Begin function compdecomp
	.type	compdecomp,@function
compdecomp:                             # @compdecomp
# %bb.0:                                # %entry
	ADDI sp,sp,-2047
	ADDI sp,sp,-17
	SW fp,2060(sp)
	ADDI fp,sp,2047
	ADDI fp,fp,17
	SW a0,-8(fp)
	SW a1,-12(fp)
	LW fp,2060(sp)
	ADDI sp,sp,2047
	ADDI sp,sp,17
	JALR zero,0(ra)
.Lfunc_end0:
	.size	compdecomp, .Lfunc_end0-compdecomp
                                        # -- End function
	.ident	"clang version 17.0.6 (https:/hub.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
