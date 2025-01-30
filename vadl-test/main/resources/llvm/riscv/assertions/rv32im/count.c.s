	.text
	.file	"count.c"
	.globl	count                           # -- Begin function count
	.type	count,@function
count:                                  # @count
# %bb.0:                                # %entry
	ADDI a1,zero,0
	ADDI a1,a1,-32
	ADD sp,sp,a1
	SW fp,28(sp)                            # 4-byte Folded Spill
	SW ra,24(sp)                            # 4-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,32
	ADD fp,sp,a1
	SW a0,-12(fp)
	ADDI a0,zero,0
	SW a0,-16(fp)
	SW a0,-20(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Inner Loop Header: Depth=1
	LW a0,-20(fp)
	LW a1,-12(fp)
	BGE a0,a1,.LBB0_4
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-16(fp)
	ADDI a0,a0,1
	SW a0,-16(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.inc
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-20(fp)
	ADDI a0,a0,1
	SW a0,-20(fp)
	JAL zero,.LBB0_1
.LBB0_4:                                # %for.end
	LW a0,-16(fp)
	LW ra,24(sp)                            # 4-byte Folded Spill
	LW fp,28(sp)                            # 4-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,32
	ADD sp,sp,a1
	JALR zero,0(ra)
.Lfunc_end0:
	.size	count, .Lfunc_end0-count
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
