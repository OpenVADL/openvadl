	.text
	.file	"recursion.c"
	.globl	recursion                       # -- Begin function recursion
	.type	recursion,@function
recursion:                              # @recursion
# %bb.0:                                # %entry
	ADDI a1,zero,0
	ADDI a1,a1,-16
	ADD sp,sp,a1
	SW fp,12(sp)                            # 4-byte Folded Spill
	SW ra,8(sp)                             # 4-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,16
	ADD fp,sp,a1
	SW a0,-16(fp)
	LW a0,-16(fp)
	ADDI a1,zero,1
	BLT a0,a1,.LBB0_2
	JAL zero,.LBB0_1
.LBB0_1:                                # %if.then
	LW a0,-16(fp)
	SW a0,-12(fp)
	JAL zero,.LBB0_3
.LBB0_2:                                # %if.end
	LW a0,-16(fp)
	ADDI a0,a0,1
	LUI ra,%hi(recursion)
	JALR ra,%lo(recursion)(ra)
	SW a0,-12(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %return
	LW a0,-12(fp)
	LW ra,8(sp)                             # 4-byte Folded Spill
	LW fp,12(sp)                            # 4-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,16
	ADD sp,sp,a1
	JALR zero,0(ra)
.Lfunc_end0:
	.size	recursion, .Lfunc_end0-recursion
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym recursion
