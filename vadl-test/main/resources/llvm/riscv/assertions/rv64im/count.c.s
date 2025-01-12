	.text
	.file	"count.c"
	.globl	count                           # -- Begin function count
	.type	count,@function
count:                                  # @count
# %bb.0:                                # %entry
	ADDI sp,sp,-32
	SD fp,24(sp)
	SD ra,16(sp)
	ADDI fp,sp,32
                                        # kill: def $x11 killed $x10
	SW a0,-20(fp)
	ADDI a0,zero,0
	SW a0,-24(fp)
	SW a0,-28(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Inner Loop Header: Depth=1
	LW a0,-28(fp)
	LW a1,-20(fp)
	BGE a0,a1,.LBB0_4
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-24(fp)
	ADDI a0,a0,1
	SW a0,-24(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.inc
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-28(fp)
	ADDI a0,a0,1
	SW a0,-28(fp)
	JAL zero,.LBB0_1
.LBB0_4:                                # %for.end
	LW a0,-24(fp)
	LD ra,16(sp)
	LD fp,24(sp)
	ADDI sp,sp,32
	JALR zero,0(ra)
.Lfunc_end0:
	.size	count, .Lfunc_end0-count
                                        # -- End function
	.ident	"clang version 17.0.6 (https:/hub.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
