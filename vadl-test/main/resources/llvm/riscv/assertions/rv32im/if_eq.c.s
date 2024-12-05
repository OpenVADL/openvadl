	.text
	.file	"if_eq.c"
	.globl	if_eq                           # -- Begin function if_eq
	.type	if_eq,@function
if_eq:                                  # @if_eq
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SW fp,12(sp)
	ADDI fp,sp,16
	SW a0,-12(fp)
	SW a1,-16(fp)
	LW tp,-12(fp)
	LW ra,-16(fp)
	BEQ ra,tp,.LBB0_2
	JAL zero,%hi(.LBB0_1)
.LBB0_1:                                # %if.then
	LW ra,-12(fp)
	SW ra,-8(fp)
	JAL zero,%hi(.LBB0_3)
.LBB0_2:                                # %if.else
	LW ra,-16(fp)
	SW ra,-8(fp)
	JAL zero,%hi(.LBB0_3)
.LBB0_3:                                # %return
	LW a0,-8(fp)
	LW fp,12(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end0:
	.size	if_eq, .Lfunc_end0-if_eq
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
