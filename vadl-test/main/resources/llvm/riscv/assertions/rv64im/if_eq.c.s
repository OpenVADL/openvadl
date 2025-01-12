	.text
	.file	"if_eq.c"
	.globl	if_eq                           # -- Begin function if_eq
	.type	if_eq,@function
if_eq:                                  # @if_eq
# %bb.0:                                # %entry
	ADDI sp,sp,-32
	SD fp,24(sp)
	SD ra,16(sp)
	ADDI fp,sp,32
                                        # kill: def $x12 killed $x11
                                        # kill: def $x12 killed $x10
	SW a0,-24(fp)
	SW a1,-28(fp)
	LWU a0,-24(fp)
	LWU a1,-28(fp)
	BNE a0,a1,.LBB0_2
	JAL zero,.LBB0_1
.LBB0_1:                                # %if.then
	LW a0,-24(fp)
	SW a0,-20(fp)
	JAL zero,.LBB0_3
.LBB0_2:                                # %if.else
	LW a0,-28(fp)
	SW a0,-20(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %return
	LW a0,-20(fp)
	LD ra,16(sp)
	LD fp,24(sp)
	ADDI sp,sp,32
	JALR zero,0(ra)
.Lfunc_end0:
	.size	if_eq, .Lfunc_end0-if_eq
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
