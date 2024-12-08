	.text
	.file	"recursion.c"
	.globl	recursion                       # -- Begin function recursion
	.type	recursion,@function
recursion:                              # @recursion
# %bb.0:                                # %entry
	ADDI sp,sp,-32
	SD sp,24(sp)
	SD fp,16(sp)
	ADDI fp,sp,32
                                        # kill: def $x1 killed $x10
	SW a0,-24(fp)
	LW ra,-24(fp)
	ADDI tp,zero,1
	BLT ra,tp,.LBB0_2
	JAL zero,%hi(.LBB0_1)
.LBB0_1:                                # %if.then
	LW ra,-24(fp)
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_3)
.LBB0_2:                                # %if.end
	LW ra,-24(fp)
	ADDI a0,ra,1
	LUI ra,%hi(recursion)
	JALR ra,%lo(recursion)(ra)
	SW a0,-20(fp)
	JAL zero,%hi(.LBB0_3)
.LBB0_3:                                # %return
	LW a0,-20(fp)
	LD fp,16(sp)
	LD sp,24(sp)
	ADDI sp,sp,32
	JALR zero,0(ra)
.Lfunc_end0:
	.size	recursion, .Lfunc_end0-recursion
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym recursion
