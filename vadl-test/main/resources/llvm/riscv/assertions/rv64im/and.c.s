	.text
	.file	"and.c"
	.globl	and                             # -- Begin function and
	.type	and,@function
and:                                    # @and
# %bb.0:                                # %entry
	ADDI a2,zero,0
	ADDI a2,a2,-32
	ADD sp,sp,a2
	SD fp,24(sp)                            # 8-byte Folded Spill
	SD ra,16(sp)                            # 8-byte Folded Spill
	ADDI a2,zero,0
	ADDI a2,a2,32
	ADD fp,sp,a2
                                        # kill: def $x12 killed $x11
                                        # kill: def $x12 killed $x10
	SW a0,-20(fp)
	SW a1,-24(fp)
	LW a0,-20(fp)
	LW a1,-24(fp)
	AND a0,a0,a1
	LD ra,16(sp)                            # 8-byte Folded Spill
	LD fp,24(sp)                            # 8-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,32
	ADD sp,sp,a1
	JALR zero,0(ra)
.Lfunc_end0:
	.size	and, .Lfunc_end0-and
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
