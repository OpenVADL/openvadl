	.text
	.file	"globals.c"
	.globl	main                            # -- Begin function main
	.type	main,@function
main:                                   # @main
# %bb.0:                                # %entry
	ADDI a0,zero,0
	ADDI a0,a0,-32
	ADD sp,sp,a0
	SD fp,24(sp)                            # 8-byte Folded Spill
	SD ra,16(sp)                            # 8-byte Folded Spill
	ADDI a0,zero,0
	ADDI a0,a0,32
	ADD fp,sp,a0
	ADDI a1,zero,0
	SW a1,-20(fp)
	LUI a0,%hi(a)
	ADDI a0,a0,%lo(a)
	SW a1,0(a0)
	LW a0,0(a0)
	SW a0,-24(fp)
	LW a0,-24(fp)
	LD ra,16(sp)                            # 8-byte Folded Spill
	LD fp,24(sp)                            # 8-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,32
	ADD sp,sp,a1
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

	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym a
