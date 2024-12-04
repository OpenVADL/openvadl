	.text
	.file	"count.c"
	.globl	count                           # -- Begin function count
	.type	count,@function
count:                                  # @count
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SW fp,12(sp)
	ADDI fp,sp,16
	SW a0,-8(fp)
	ADDI ra,zero,0
	SW ra,-12(fp)
	SW ra,-16(fp)
	JAL zero,%hi(.LBB0_1)
.LBB0_1:                                # %for.cond
                                        # =>This Inner Loop Header: Depth=1
	LW ra,-16(fp)
	LW tp,-8(fp)
	BGE ra,tp,.LBB0_4
	JAL zero,%hi(.LBB0_2)
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	LW ra,-12(fp)
	ADDI ra,ra,1
	SW ra,-12(fp)
	JAL zero,%hi(.LBB0_3)
.LBB0_3:                                # %for.inc
                                        #   in Loop: Header=BB0_1 Depth=1
	LW ra,-16(fp)
	ADDI ra,ra,1
	SW ra,-16(fp)
	JAL zero,%hi(.LBB0_1)
.LBB0_4:                                # %for.end
	LW a0,-12(fp)
	LW fp,12(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end0:
	.size	count, .Lfunc_end0-count
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
