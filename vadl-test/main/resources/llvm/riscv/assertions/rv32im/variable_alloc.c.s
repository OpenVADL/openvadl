	.text
	.file	"variable_alloc.c"
	.globl	compdecomp                      # -- Begin function compdecomp
	.type	compdecomp,@function
compdecomp:                             # @compdecomp
# %bb.0:                                # %entry
	LUI a2,0xfffff
	ADDI a2,a2,2032
	ADD sp,sp,a2
	LUI a2,0x1
	ADDI a2,a2,-2036
	ADD a2,sp,a2
	SW fp,0(a2)                             # 4-byte Folded Spill
	LUI a2,0x1
	ADDI a2,a2,-2040
	ADD a2,sp,a2
	SW ra,0(a2)                             # 4-byte Folded Spill
	LUI a2,0x1
	ADDI a2,a2,-2032
	ADD fp,sp,a2
	SW a0,-12(fp)
	SW a1,-16(fp)
	LUI a0,0x1
	ADDI a0,a0,-2040
	ADD a0,sp,a0
	LW ra,0(a0)                             # 4-byte Folded Spill
	LUI a0,0x1
	ADDI a0,a0,-2036
	ADD a0,sp,a0
	LW fp,0(a0)                             # 4-byte Folded Spill
	LUI a0,0x1
	ADDI a0,a0,-2032
	ADD sp,sp,a0
	JALR zero,0(ra)
.Lfunc_end0:
	.size	compdecomp, .Lfunc_end0-compdecomp
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
