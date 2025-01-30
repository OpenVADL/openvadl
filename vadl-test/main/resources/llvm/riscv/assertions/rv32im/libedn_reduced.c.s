	.text
	.file	"libedn_reduced.c"
	.globl	jpegdct                         # -- Begin function jpegdct
	.type	jpegdct,@function
jpegdct:                                # @jpegdct
# %bb.0:                                # %entry
	ADDI a2,zero,0
	ADDI a2,a2,-80
	ADD sp,sp,a2
	SW fp,76(sp)                            # 4-byte Folded Spill
	SW ra,72(sp)                            # 4-byte Folded Spill
	ADDI a2,zero,0
	ADDI a2,a2,80
	ADD fp,sp,a2
	SW a0,-12(fp)
	SW a1,-16(fp)
	ADDI a0,zero,1
	SH a0,-70(fp)
	ADDI a0,zero,0
	SH a0,-72(fp)
	ADDI a0,zero,13
	SH a0,-74(fp)
	ADDI a0,zero,8
	SH a0,-76(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_3 Depth 2
                                        #       Child Loop BB0_5 Depth 3
	LH a1,-70(fp)
	ADDI a0,zero,8
	BLT a0,a1,.LBB0_12
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	ADDI a0,zero,0
	SH a0,-66(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.cond2
                                        #   Parent Loop BB0_1 Depth=1
                                        # =>  This Loop Header: Depth=2
                                        #       Child Loop BB0_5 Depth 3
	LH a1,-66(fp)
	ADDI a0,zero,7
	BLT a0,a1,.LBB0_10
	JAL zero,.LBB0_4
.LBB0_4:                                # %for.body6
                                        #   in Loop: Header=BB0_3 Depth=2
	ADDI a0,zero,0
	SH a0,-68(fp)
	JAL zero,.LBB0_5
.LBB0_5:                                # %for.cond7
                                        #   Parent Loop BB0_1 Depth=1
                                        #     Parent Loop BB0_3 Depth=2
                                        # =>    This Inner Loop Header: Depth=3
	LH a1,-68(fp)
	ADDI a0,zero,3
	BLT a0,a1,.LBB0_8
	JAL zero,.LBB0_6
.LBB0_6:                                # %for.body11
                                        #   in Loop: Header=BB0_5 Depth=3
	LW a3,-12(fp)
	LH a4,-70(fp)
	LH a0,-68(fp)
	MUL a1,a4,a0
	SLLI a1,a1,1
	ADD a1,a3,a1
	LH a1,0(a1)
	ADDI a2,zero,7
	SUB a5,a2,a0
	MUL a4,a4,a5
	SLLI a4,a4,1
	ADD a3,a3,a4
	LH a3,0(a3)
	ADD a3,a1,a3
	SLLI a1,a0,2
	ADDI a0,fp,-64
	ADD a1,a0,a1
	SW a3,0(a1)
	LW a3,-12(fp)
	LH a4,-70(fp)
	LH a5,-68(fp)
	MUL a1,a4,a5
	SLLI a1,a1,1
	ADD a1,a3,a1
	LH a1,0(a1)
	SUB a2,a2,a5
	MUL a4,a4,a2
	SLLI a4,a4,1
	ADD a3,a3,a4
	LH a3,0(a3)
	SUB a1,a1,a3
	SLLI a2,a2,2
	ADD a0,a0,a2
	SW a1,0(a0)
	JAL zero,.LBB0_7
.LBB0_7:                                # %for.inc
                                        #   in Loop: Header=BB0_5 Depth=3
	LH a0,-68(fp)
	ADDI a0,a0,1
	SH a0,-68(fp)
	JAL zero,.LBB0_5
.LBB0_8:                                # %for.end
                                        #   in Loop: Header=BB0_3 Depth=2
	LW a0,-64(fp)
	LW a1,-52(fp)
	ADD a0,a0,a1
	SW a0,-32(fp)
	LW a0,-64(fp)
	LW a1,-52(fp)
	SUB a0,a0,a1
	SW a0,-28(fp)
	LW a0,-60(fp)
	LW a1,-56(fp)
	ADD a0,a0,a1
	SW a0,-24(fp)
	LW a0,-60(fp)
	LW a1,-56(fp)
	SUB a0,a0,a1
	SW a0,-20(fp)
	LW a0,-32(fp)
	LW a1,-24(fp)
	ADD a0,a0,a1
	LH a1,-72(fp)
	SRA a1,a0,a1
	LW a0,-12(fp)
	SH a1,0(a0)
	LW a0,-32(fp)
	LW a1,-24(fp)
	SUB a0,a0,a1
	LH a1,-72(fp)
	SRA a1,a0,a1
	LW a0,-12(fp)
	LH a2,-70(fp)
	SLLI a2,a2,3
	ADD a0,a0,a2
	SH a1,0(a0)
	LW a0,-20(fp)
	LW a1,-28(fp)
	ADD a0,a0,a1
	ADDI a1,zero,16
	SW a1,-80(fp)                           # 4-byte Folded Spill
	SLL a0,a0,a1
	SRA a0,a0,a1
	LW a2,-16(fp)
	LH a2,20(a2)
	MUL a0,a0,a2
	SW a0,-32(fp)
	LW a0,-32(fp)
	LW a2,-28(fp)
	LW a3,-16(fp)
	LH a3,18(a3)
	MUL a2,a2,a3
	LH a3,-74(fp)
	SRA a2,a2,a3
	ADD a2,a0,a2
	LW a0,-12(fp)
	LH a3,-70(fp)
	SLLI a3,a3,2
	ADD a0,a0,a3
	SH a2,0(a0)
	LW a0,-32(fp)
	LW a2,-20(fp)
	LW a3,-16(fp)
	LH a3,22(a3)
	MUL a2,a2,a3
	LH a3,-74(fp)
	SRA a2,a2,a3
	ADD a2,a0,a2
	LW a0,-12(fp)
	LH a3,-70(fp)
	ADDI a4,zero,12
	MUL a3,a3,a4
	ADD a0,a0,a3
	SH a2,0(a0)
	LW a0,-48(fp)
	LW a2,-36(fp)
	ADD a0,a0,a2
	SLL a0,a0,a1
	SRA a0,a0,a1
	LW a2,-16(fp)
	LH a2,4(a2)
	MUL a0,a0,a2
	SW a0,-64(fp)
	LW a0,-44(fp)
	LW a2,-40(fp)
	ADD a0,a0,a2
	SLL a0,a0,a1
	SRA a0,a0,a1
	LW a2,-16(fp)
	LH a2,0(a2)
	MUL a0,a0,a2
	SW a0,-60(fp)
	LW a0,-48(fp)
	LW a2,-40(fp)
	ADD a0,a0,a2
	SW a0,-56(fp)
	LW a0,-44(fp)
	LW a2,-36(fp)
	ADD a0,a0,a2
	SW a0,-52(fp)
	LW a0,-56(fp)
	LW a2,-52(fp)
	ADD a0,a0,a2
	SLL a0,a0,a1
	SRA a0,a0,a1
	LW a2,-16(fp)
	LH a2,16(a2)
	MUL a0,a0,a2
	SW a0,-32(fp)
	LH a0,-56(fp)
	LW a2,-16(fp)
	LH a2,2(a2)
	MUL a0,a0,a2
	LW a2,-32(fp)
	ADD a0,a0,a2
	SW a0,-56(fp)
	LH a0,-52(fp)
	LW a2,-16(fp)
	LH a2,6(a2)
	MUL a0,a0,a2
	LW a2,-32(fp)
	ADD a0,a0,a2
	SW a0,-52(fp)
	LW a0,-48(fp)
	LW a2,-16(fp)
	ADDI a2,a2,8
	LH a2,0(a2)
	MUL a0,a0,a2
	LW a2,-64(fp)
	ADD a0,a0,a2
	LW a2,-56(fp)
	ADD a0,a0,a2
	SLL a0,a0,a1
	SRA a0,a0,a1
	LH a2,-74(fp)
	SRA a2,a0,a2
	LW a0,-12(fp)
	LH a3,-70(fp)
	ADDI a4,zero,14
	MUL a3,a3,a4
	ADD a0,a0,a3
	SH a2,0(a0)
	LW a0,-44(fp)
	LW a2,-16(fp)
	ADDI a2,a2,12
	LH a2,0(a2)
	MUL a0,a0,a2
	LW a2,-60(fp)
	ADD a0,a0,a2
	LW a2,-52(fp)
	ADD a0,a0,a2
	SLL a0,a0,a1
	SRA a0,a0,a1
	LH a2,-74(fp)
	SRA a2,a0,a2
	LW a0,-12(fp)
	LH a3,-70(fp)
	ADDI a4,zero,10
	MUL a3,a3,a4
	ADD a0,a0,a3
	SH a2,0(a0)
	LW a0,-40(fp)
	LW a2,-16(fp)
	ADDI a2,a2,10
	LH a2,0(a2)
	MUL a0,a0,a2
	LW a2,-60(fp)
	ADD a0,a0,a2
	LW a2,-56(fp)
	ADD a0,a0,a2
	SLL a0,a0,a1
	SRA a0,a0,a1
	LH a2,-74(fp)
	SRA a2,a0,a2
	LW a0,-12(fp)
	LH a3,-70(fp)
	ADDI a4,zero,6
	MUL a3,a3,a4
	ADD a0,a0,a3
	SH a2,0(a0)
	LW a0,-36(fp)
	LW a2,-16(fp)
	ADDI a2,a2,14
	LH a2,0(a2)
	MUL a0,a0,a2
	LW a2,-64(fp)
	ADD a0,a0,a2
	LW a2,-52(fp)
	ADD a0,a0,a2
	SLL a0,a0,a1
	SRA a0,a0,a1
	LH a1,-74(fp)
	SRA a1,a0,a1
	LW a0,-12(fp)
	LH a2,-70(fp)
	SLLI a2,a2,1
	ADD a0,a0,a2
	SH a1,0(a0)
	JAL zero,.LBB0_9
.LBB0_9:                                # %for.inc221
                                        #   in Loop: Header=BB0_3 Depth=2
	LH a0,-66(fp)
	ADDI a0,a0,1
	SH a0,-66(fp)
	LH a1,-76(fp)
	LW a0,-12(fp)
	SLLI a1,a1,1
	ADD a0,a0,a1
	SW a0,-12(fp)
	JAL zero,.LBB0_3
.LBB0_10:                               # %for.end224
                                        #   in Loop: Header=BB0_1 Depth=1
	JAL zero,.LBB0_11
.LBB0_11:                               # %for.inc225
                                        #   in Loop: Header=BB0_1 Depth=1
	LH a0,-70(fp)
	ADDI a0,a0,7
	SH a0,-70(fp)
	LH a0,-72(fp)
	ADDI a0,a0,3
	SH a0,-72(fp)
	LH a0,-74(fp)
	ADDI a0,a0,3
	SH a0,-74(fp)
	LH a0,-76(fp)
	ADDI a0,a0,-7
	SH a0,-76(fp)
	LW a0,-12(fp)
	ADDI a0,a0,-128
	SW a0,-12(fp)
	JAL zero,.LBB0_1
.LBB0_12:                               # %for.end239
	LW ra,72(sp)                            # 4-byte Folded Spill
	LW fp,76(sp)                            # 4-byte Folded Spill
	ADDI a0,zero,0
	ADDI a0,a0,80
	ADD sp,sp,a0
	JALR zero,0(ra)
.Lfunc_end0:
	.size	jpegdct, .Lfunc_end0-jpegdct
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
