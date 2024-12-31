	.text
	.file	"and.c"
	.globl	and                             # -- Begin function and
	.type	and,@function
and:                                    # @and
# %bb.0:                                # %entry
	ADDI sp,sp,-32
	SD fp,24(sp)
	SD ra,16(sp)
	ADDI fp,sp,32
                                        # kill: def $x12 killed $x11
                                        # kill: def $x12 killed $x10
	SW a0,-20(fp)
	SW a1,-24(fp)
	LW a0,-20(fp)
	LW a1,-24(fp)
	AND a0,a0,a1
	LD ra,16(sp)
	LD fp,24(sp)
	ADDI sp,sp,32
	JALR zero,0(ra)
.Lfunc_end0:
	.size	and, .Lfunc_end0-and
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
