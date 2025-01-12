	.text
	.file	"large_constant_with_compare.c"
	.globl	main                            # -- Begin function main
	.type	main,@function
main:                                   # @main
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SW fp,12(sp)
	SW ra,8(sp)
	ADDI fp,sp,16
	ADDI a0,zero,0
	SW a0,-12(fp)
	ADDI a0,zero,1
	SW a0,-16(fp)
	LW a0,-16(fp)
	XORI a0,a0,1
	SLTU a0,zero,a0
	LW ra,8(sp)
	LW fp,12(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end0:
	.size	main, .Lfunc_end0-main
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
