	.text
	.file	"globals.c"
	.globl	main                            # -- Begin function main
	.type	main,@function
main:                                   # @main
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SD fp,8(sp)
	ADDI fp,sp,16
	ADDI tp,zero,0
	SW tp,-12(fp)
	LUI ra,%hi(a)
	ADDI ra,ra,%lo(a)
	SW tp,0(ra)
	LW ra,0(ra)
	SW ra,-16(fp)
	LW a0,-16(fp)
	LD fp,8(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
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

	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym a
