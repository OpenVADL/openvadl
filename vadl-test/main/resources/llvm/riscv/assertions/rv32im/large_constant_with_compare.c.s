	.text
	.file	"large_constant_with_compare.c"
	.globl	main                            # -- Begin function main
	.type	main,@function
main:                                   # @main
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SW fp,12(sp)
	ADDI fp,sp,16
	ADDI ra,zero,0
	SW ra,-8(fp)
	ADDI ra,zero,1
	SW ra,-12(fp)
	LW ra,-12(fp)
	XORI ra,ra,1
	SLTU a0,zero,ra
	LW fp,12(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end0:
	.size	main, .Lfunc_end0-main
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
