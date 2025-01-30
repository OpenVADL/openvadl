	.text
	.file	"libedn_reduced.c"
	.globl	jpegdct                         # -- Begin function jpegdct
	.type	jpegdct,@function
jpegdct:                                # @jpegdct
# %bb.0:                                # %entry
	ADDI a2,zero,0
	ADDI a2,a2,-128
	ADD sp,sp,a2
	SD fp,120(sp)                           # 8-byte Folded Spill
	SD ra,112(sp)                           # 8-byte Folded Spill
	ADDI a2,zero,0
	ADDI a2,a2,128
	ADD fp,sp,a2
	SD a0,-24(fp)
	SD a1,-32(fp)
	ADDI a0,zero,1
	SH a0,-86(fp)
	ADDI a0,zero,0
	SH a0,-88(fp)
	ADDI a0,zero,13
	SH a0,-90(fp)
	ADDI a0,zero,8
	SH a0,-92(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_3 Depth 2
                                        #       Child Loop BB0_5 Depth 3
	LH a1,-86(fp)
	ADDI a0,zero,8
	BLT a0,a1,.LBB0_12
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	ADDI a0,zero,0
	SH a0,-82(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.cond2
                                        #   Parent Loop BB0_1 Depth=1
                                        # =>  This Loop Header: Depth=2
                                        #       Child Loop BB0_5 Depth 3
	LH a1,-82(fp)
	ADDI a0,zero,7
	BLT a0,a1,.LBB0_10
	JAL zero,.LBB0_4
.LBB0_4:                                # %for.body6
                                        #   in Loop: Header=BB0_3 Depth=2
	ADDI a0,zero,0
	SH a0,-84(fp)
	JAL zero,.LBB0_5
.LBB0_5:                                # %for.cond7
                                        #   Parent Loop BB0_1 Depth=1
                                        #     Parent Loop BB0_3 Depth=2
                                        # =>    This Inner Loop Header: Depth=3
	LH a1,-84(fp)
	ADDI a0,zero,3
	BLT a0,a1,.LBB0_8
	JAL zero,.LBB0_6
.LBB0_6:                                # %for.body11
                                        #   in Loop: Header=BB0_5 Depth=3
	LD a3,-24(fp)
	LH a4,-86(fp)
	LH a0,-84(fp)
	MUL a1,a4,a0
	SLLI a1,a1,1
	ADD a1,a3,a1
	LH a1,0(a1)
	ADDI a2,zero,7
	SUB a5,a2,a0
	MUL a4,a4,a5
	ADDI a5,zero,32
	SLL a4,a4,a5
	SRA a4,a4,a5
	SLLI a4,a4,1
	ADD a3,a3,a4
	LH a3,0(a3)
	ADD a3,a1,a3
	SLLI a1,a0,2
	ADDI a0,fp,-80
	ADD a1,a0,a1
	SW a3,0(a1)
	LD a3,-24(fp)
	LH a4,-86(fp)
	LH a6,-84(fp)
	MUL a1,a4,a6
	SLLI a1,a1,1
	ADD a1,a3,a1
	LH a1,0(a1)
	SUB a2,a2,a6
	MUL a4,a4,a2
	SLL a4,a4,a5
	SRA a4,a4,a5
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
	LH a0,-84(fp)
	ADDI a0,a0,1
	SH a0,-84(fp)
	JAL zero,.LBB0_5
.LBB0_8:                                # %for.end
                                        #   in Loop: Header=BB0_3 Depth=2
	LW a0,-80(fp)
	LW a1,-68(fp)
	ADD a0,a0,a1
	SW a0,-48(fp)
	LW a0,-80(fp)
	LW a1,-68(fp)
	SUB a0,a0,a1
	SW a0,-44(fp)
	LW a0,-76(fp)
	LW a1,-72(fp)
	ADD a0,a0,a1
	SW a0,-40(fp)
	LW a0,-76(fp)
	LW a1,-72(fp)
	SUB a0,a0,a1
	SW a0,-36(fp)
	LW a0,-48(fp)
	LW a1,-40(fp)
	ADD a0,a0,a1
	ADDI a4,zero,32
	SD a4,-116(fp)                          # 8-byte Folded Spill
	SLL a0,a0,a4
	SRA a0,a0,a4
	LH a1,-88(fp)
	ADDI a2,zero,1
	SLLI a2,a2,32
	ADDI a2,a2,-1
	SD a2,-108(fp)                          # 8-byte Folded Spill
	AND a1,a1,a2
	SRA a1,a0,a1
	LD a0,-24(fp)
	SH a1,0(a0)
	LW a0,-48(fp)
	LW a1,-40(fp)
	SUB a0,a0,a1
	SLL a0,a0,a4
	SRA a0,a0,a4
	LH a1,-88(fp)
	AND a1,a1,a2
	SRA a1,a0,a1
	LD a0,-24(fp)
	LH a3,-86(fp)
	SLLI a3,a3,3
	ADD a0,a0,a3
	SH a1,0(a0)
	LW a0,-36(fp)
	LW a1,-44(fp)
	ADD a0,a0,a1
	ADDI a1,zero,48
	SD a1,-100(fp)                          # 8-byte Folded Spill
	SLL a0,a0,a1
	SRA a0,a0,a1
	LD a3,-32(fp)
	LH a3,20(a3)
	MUL a0,a0,a3
	SW a0,-48(fp)
	LW a0,-48(fp)
	LW a3,-44(fp)
	LD a5,-32(fp)
	LH a5,18(a5)
	MUL a3,a3,a5
	SLL a3,a3,a4
	SRA a3,a3,a4
	LH a5,-90(fp)
	AND a5,a5,a2
	SRA a3,a3,a5
	ADD a3,a0,a3
	LD a0,-24(fp)
	LH a5,-86(fp)
	SLLI a5,a5,2
	ADD a0,a0,a5
	SH a3,0(a0)
	LW a0,-48(fp)
	LW a3,-36(fp)
	LD a5,-32(fp)
	LH a5,22(a5)
	MUL a3,a3,a5
	SLL a3,a3,a4
	SRA a3,a3,a4
	LH a4,-90(fp)
	AND a4,a4,a2
	SRA a3,a3,a4
	ADD a3,a0,a3
	LD a0,-24(fp)
	LH a4,-86(fp)
	ADDI a5,zero,12
	MUL a4,a4,a5
	ADD a0,a0,a4
	SH a3,0(a0)
	LW a0,-64(fp)
	LW a3,-52(fp)
	ADD a0,a0,a3
	SLL a0,a0,a1
	SRA a0,a0,a1
	LD a3,-32(fp)
	LH a3,4(a3)
	MUL a0,a0,a3
	SW a0,-80(fp)
	LW a0,-60(fp)
	LW a3,-56(fp)
	ADD a0,a0,a3
	SLL a0,a0,a1
	SRA a0,a0,a1
	LD a3,-32(fp)
	LH a3,0(a3)
	MUL a0,a0,a3
	SW a0,-76(fp)
	LW a0,-64(fp)
	LW a3,-56(fp)
	ADD a0,a0,a3
	SW a0,-72(fp)
	LW a0,-60(fp)
	LW a3,-52(fp)
	ADD a0,a0,a3
	SW a0,-68(fp)
	LW a0,-72(fp)
	LW a3,-68(fp)
	ADD a0,a0,a3
	SLL a0,a0,a1
	SRA a0,a0,a1
	LD a3,-32(fp)
	LH a3,16(a3)
	MUL a0,a0,a3
	SW a0,-48(fp)
	LH a0,-72(fp)
	LD a3,-32(fp)
	LH a3,2(a3)
	MUL a0,a0,a3
	LW a3,-48(fp)
	ADD a0,a0,a3
	SW a0,-72(fp)
	LH a0,-68(fp)
	LD a3,-32(fp)
	LH a3,6(a3)
	MUL a0,a0,a3
	LW a3,-48(fp)
	ADD a0,a0,a3
	SW a0,-68(fp)
	LW a0,-64(fp)
	LD a3,-32(fp)
	ADDI a3,a3,8
	LH a3,0(a3)
	MUL a0,a0,a3
	LW a3,-80(fp)
	ADD a0,a0,a3
	LW a3,-72(fp)
	ADD a0,a0,a3
	SLL a0,a0,a1
	SRA a0,a0,a1
	LH a3,-90(fp)
	AND a3,a3,a2
	SRA a3,a0,a3
	LD a0,-24(fp)
	LH a4,-86(fp)
	ADDI a5,zero,14
	MUL a4,a4,a5
	ADD a0,a0,a4
	SH a3,0(a0)
	LW a0,-60(fp)
	LD a3,-32(fp)
	ADDI a3,a3,12
	LH a3,0(a3)
	MUL a0,a0,a3
	LW a3,-76(fp)
	ADD a0,a0,a3
	LW a3,-68(fp)
	ADD a0,a0,a3
	SLL a0,a0,a1
	SRA a0,a0,a1
	LH a3,-90(fp)
	AND a3,a3,a2
	SRA a3,a0,a3
	LD a0,-24(fp)
	LH a4,-86(fp)
	ADDI a5,zero,10
	MUL a4,a4,a5
	ADD a0,a0,a4
	SH a3,0(a0)
	LW a0,-56(fp)
	LD a3,-32(fp)
	ADDI a3,a3,10
	LH a3,0(a3)
	MUL a0,a0,a3
	LW a3,-76(fp)
	ADD a0,a0,a3
	LW a3,-72(fp)
	ADD a0,a0,a3
	SLL a0,a0,a1
	SRA a0,a0,a1
	LH a3,-90(fp)
	AND a3,a3,a2
	SRA a3,a0,a3
	LD a0,-24(fp)
	LH a4,-86(fp)
	ADDI a5,zero,6
	MUL a4,a4,a5
	ADD a0,a0,a4
	SH a3,0(a0)
	LW a0,-52(fp)
	LD a3,-32(fp)
	ADDI a3,a3,14
	LH a3,0(a3)
	MUL a0,a0,a3
	LW a3,-80(fp)
	ADD a0,a0,a3
	LW a3,-68(fp)
	ADD a0,a0,a3
	SLL a0,a0,a1
	SRA a0,a0,a1
	LH a1,-90(fp)
	AND a1,a1,a2
	SRA a1,a0,a1
	LD a0,-24(fp)
	LH a2,-86(fp)
	SLLI a2,a2,1
	ADD a0,a0,a2
	SH a1,0(a0)
	JAL zero,.LBB0_9
.LBB0_9:                                # %for.inc221
                                        #   in Loop: Header=BB0_3 Depth=2
	LH a0,-82(fp)
	ADDI a0,a0,1
	SH a0,-82(fp)
	LH a1,-92(fp)
	LD a0,-24(fp)
	SLLI a1,a1,1
	ADD a0,a0,a1
	SD a0,-24(fp)
	JAL zero,.LBB0_3
.LBB0_10:                               # %for.end224
                                        #   in Loop: Header=BB0_1 Depth=1
	JAL zero,.LBB0_11
.LBB0_11:                               # %for.inc225
                                        #   in Loop: Header=BB0_1 Depth=1
	LH a0,-86(fp)
	ADDI a0,a0,7
	SH a0,-86(fp)
	LH a0,-88(fp)
	ADDI a0,a0,3
	SH a0,-88(fp)
	LH a0,-90(fp)
	ADDI a0,a0,3
	SH a0,-90(fp)
	LH a0,-92(fp)
	ADDI a0,a0,-7
	SH a0,-92(fp)
	LD a0,-24(fp)
	ADDI a0,a0,-128
	SD a0,-24(fp)
	JAL zero,.LBB0_1
.LBB0_12:                               # %for.end239
	LD ra,112(sp)                           # 8-byte Folded Spill
	LD fp,120(sp)                           # 8-byte Folded Spill
	ADDI a0,zero,0
	ADDI a0,a0,128
	ADD sp,sp,a0
	JALR zero,0(ra)
.Lfunc_end0:
	.size	jpegdct, .Lfunc_end0-jpegdct
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
