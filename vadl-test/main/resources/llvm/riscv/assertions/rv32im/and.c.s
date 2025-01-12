	.text
	.file	"and.c"
	.globl	and                             # -- Begin function and
	.type	and,@function
and:                                    # @and
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SW fp,12(sp)
	SW ra,8(sp)
	ADDI fp,sp,16
	SW a0,-12(fp)
	SW a1,-16(fp)
	LW a0,-12(fp)
	LW a1,-16(fp)
	AND a0,a0,a1
	LW ra,8(sp)
	LW fp,12(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end0:
	.size	and, .Lfunc_end0-and
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
