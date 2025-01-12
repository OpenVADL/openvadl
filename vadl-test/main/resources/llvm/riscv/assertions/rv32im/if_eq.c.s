	.text
	.file	"if_eq.c"
	.globl	if_eq                           # -- Begin function if_eq
	.type	if_eq,@function
if_eq:                                  # @if_eq
# %bb.0:                                # %entry
	ADDI sp,sp,-32
	SW fp,28(sp)
	SW ra,24(sp)
	ADDI fp,sp,32
	SW a0,-16(fp)
	SW a1,-20(fp)
	LW a0,-16(fp)
	LW a1,-20(fp)
	BNE a0,a1,.LBB0_2
	JAL zero,.LBB0_1
.LBB0_1:                                # %if.then
	LW a0,-16(fp)
	SW a0,-12(fp)
	JAL zero,.LBB0_3
.LBB0_2:                                # %if.else
	LW a0,-20(fp)
	SW a0,-12(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %return
	LW a0,-12(fp)
	LW ra,24(sp)
	LW fp,28(sp)
	ADDI sp,sp,32
	JALR zero,0(ra)
.Lfunc_end0:
	.size	if_eq, .Lfunc_end0-if_eq
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
