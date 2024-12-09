	.text
	.file	"libedn_reduced.c"
	.globl	jpegdct                         # -- Begin function jpegdct
	.type	jpegdct,@function
jpegdct:                                # @jpegdct
# %bb.0:                                # %entry
	ADDI sp,sp,-112
	SD fp,104(sp)
	ADDI fp,sp,112
	SD a0,-16(fp)
	SD a1,-24(fp)
	ADDI ra,zero,1
	SH ra,-78(fp)
	ADDI ra,zero,0
	SH ra,-80(fp)
	ADDI ra,zero,13
	SH ra,-82(fp)
	ADDI ra,zero,8
	SH ra,-84(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_3 Depth 2
                                        #       Child Loop BB0_5 Depth 3
	LH tp,-78(fp)
	ADDI ra,zero,8
	BLT ra,tp,.LBB0_12
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	ADDI ra,zero,0
	SH ra,-74(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.cond2
                                        #   Parent Loop BB0_1 Depth=1
                                        # =>  This Loop Header: Depth=2
                                        #       Child Loop BB0_5 Depth 3
	LH tp,-74(fp)
	ADDI ra,zero,7
	BLT ra,tp,.LBB0_10
	JAL zero,.LBB0_4
.LBB0_4:                                # %for.body6
                                        #   in Loop: Header=BB0_3 Depth=2
	ADDI ra,zero,0
	SH ra,-76(fp)
	JAL zero,.LBB0_5
.LBB0_5:                                # %for.cond7
                                        #   Parent Loop BB0_1 Depth=1
                                        #     Parent Loop BB0_3 Depth=2
                                        # =>    This Inner Loop Header: Depth=3
	LH tp,-76(fp)
	ADDI ra,zero,3
	BLT ra,tp,.LBB0_8
	JAL zero,.LBB0_6
.LBB0_6:                                # %for.body11
                                        #   in Loop: Header=BB0_5 Depth=3
	LD t1,-16(fp)
	LH t2,-78(fp)
	LH ra,-76(fp)
	MUL tp,t2,ra
	SLLI tp,tp,1
	ADD tp,t1,tp
	LH tp,0(tp)
	ADDI t0,zero,7
	SUB a0,t0,ra
	MUL t2,t2,a0
	ADDI a0,zero,32
	SLL t2,t2,a0
	SRA t2,t2,a0
	SLLI t2,t2,1
	ADD t1,t1,t2
	LH t1,0(t1)
	ADD t1,tp,t1
	SLLI tp,ra,2
	ADDI ra,fp,-72
	ADD tp,ra,tp
	SW t1,0(tp)
	LD t1,-16(fp)
	LH t2,-78(fp)
	LH a1,-76(fp)
	MUL tp,t2,a1
	SLLI tp,tp,1
	ADD tp,t1,tp
	LH tp,0(tp)
	SUB t0,t0,a1
	MUL t2,t2,t0
	SLL t2,t2,a0
	SRA t2,t2,a0
	SLLI t2,t2,1
	ADD t1,t1,t2
	LH t1,0(t1)
	SUB tp,tp,t1
	SLLI t0,t0,2
	ADD ra,ra,t0
	SW tp,0(ra)
	JAL zero,.LBB0_7
.LBB0_7:                                # %for.inc
                                        #   in Loop: Header=BB0_5 Depth=3
	LH ra,-76(fp)
	ADDI ra,ra,1
	SH ra,-76(fp)
	JAL zero,.LBB0_5
.LBB0_8:                                # %for.end
                                        #   in Loop: Header=BB0_3 Depth=2
	LW ra,-72(fp)
	LW tp,-60(fp)
	ADD ra,ra,tp
	SW ra,-40(fp)
	LW ra,-72(fp)
	LW tp,-60(fp)
	SUB ra,ra,tp
	SW ra,-36(fp)
	LW ra,-68(fp)
	LW tp,-64(fp)
	ADD ra,ra,tp
	SW ra,-32(fp)
	LW ra,-68(fp)
	LW tp,-64(fp)
	SUB ra,ra,tp
	SW ra,-28(fp)
	LW ra,-40(fp)
	LW tp,-32(fp)
	ADD ra,ra,tp
	ADDI t2,zero,32
	SD t2,-108(fp)
	SLL ra,ra,t2
	SRA ra,ra,t2
	LH tp,-80(fp)
	LUI t0,0xfffff
	ADDI t0,t0,4095
	ADDI t0,t0,0
	SD t0,-100(fp)
	AND tp,tp,t0
	SRA tp,ra,tp
	LD ra,-16(fp)
	SH tp,0(ra)
	LW ra,-40(fp)
	LW tp,-32(fp)
	SUB ra,ra,tp
	SLL ra,ra,t2
	SRA ra,ra,t2
	LH tp,-80(fp)
	AND tp,tp,t0
	SRA tp,ra,tp
	LD ra,-16(fp)
	LH t1,-78(fp)
	SLLI t1,t1,3
	ADD ra,ra,t1
	SH tp,0(ra)
	LW ra,-28(fp)
	LW tp,-36(fp)
	ADD ra,ra,tp
	ADDI tp,zero,48
	SD tp,-92(fp)
	SLL ra,ra,tp
	SRA ra,ra,tp
	LD t1,-24(fp)
	LH t1,20(t1)
	MUL ra,ra,t1
	SW ra,-40(fp)
	LW ra,-40(fp)
	LW t1,-36(fp)
	LD a0,-24(fp)
	LH a0,18(a0)
	MUL t1,t1,a0
	SLL t1,t1,t2
	SRA t1,t1,t2
	LH a0,-82(fp)
	AND a0,a0,t0
	SRA t1,t1,a0
	ADD t1,ra,t1
	LD ra,-16(fp)
	LH a0,-78(fp)
	SLLI a0,a0,2
	ADD ra,ra,a0
	SH t1,0(ra)
	LW ra,-40(fp)
	LW t1,-28(fp)
	LD a0,-24(fp)
	LH a0,22(a0)
	MUL t1,t1,a0
	SLL t1,t1,t2
	SRA t1,t1,t2
	LH t2,-82(fp)
	AND t2,t2,t0
	SRA t1,t1,t2
	ADD t1,ra,t1
	LD ra,-16(fp)
	LH t2,-78(fp)
	ADDI a0,zero,12
	MUL t2,t2,a0
	ADD ra,ra,t2
	SH t1,0(ra)
	LW ra,-56(fp)
	LW t1,-44(fp)
	ADD ra,ra,t1
	SLL ra,ra,tp
	SRA ra,ra,tp
	LD t1,-24(fp)
	LH t1,4(t1)
	MUL ra,ra,t1
	SW ra,-72(fp)
	LW ra,-52(fp)
	LW t1,-48(fp)
	ADD ra,ra,t1
	SLL ra,ra,tp
	SRA ra,ra,tp
	LD t1,-24(fp)
	LH t1,0(t1)
	MUL ra,ra,t1
	SW ra,-68(fp)
	LW ra,-56(fp)
	LW t1,-48(fp)
	ADD ra,ra,t1
	SW ra,-64(fp)
	LW ra,-52(fp)
	LW t1,-44(fp)
	ADD ra,ra,t1
	SW ra,-60(fp)
	LW ra,-64(fp)
	LW t1,-60(fp)
	ADD ra,ra,t1
	SLL ra,ra,tp
	SRA ra,ra,tp
	LD t1,-24(fp)
	LH t1,16(t1)
	MUL ra,ra,t1
	SW ra,-40(fp)
	LH ra,-64(fp)
	LD t1,-24(fp)
	LH t1,2(t1)
	MUL ra,ra,t1
	LW t1,-40(fp)
	ADD ra,ra,t1
	SW ra,-64(fp)
	LH ra,-60(fp)
	LD t1,-24(fp)
	LH t1,6(t1)
	MUL ra,ra,t1
	LW t1,-40(fp)
	ADD ra,ra,t1
	SW ra,-60(fp)
	LW ra,-56(fp)
	LD t1,-24(fp)
	ADDI t1,t1,8
	LH t1,0(t1)
	MUL ra,ra,t1
	LW t1,-72(fp)
	ADD ra,ra,t1
	LW t1,-64(fp)
	ADD ra,ra,t1
	SLL ra,ra,tp
	SRA ra,ra,tp
	LH t1,-82(fp)
	AND t1,t1,t0
	SRA t1,ra,t1
	LD ra,-16(fp)
	LH t2,-78(fp)
	ADDI a0,zero,14
	MUL t2,t2,a0
	ADD ra,ra,t2
	SH t1,0(ra)
	LW ra,-52(fp)
	LD t1,-24(fp)
	ADDI t1,t1,12
	LH t1,0(t1)
	MUL ra,ra,t1
	LW t1,-68(fp)
	ADD ra,ra,t1
	LW t1,-60(fp)
	ADD ra,ra,t1
	SLL ra,ra,tp
	SRA ra,ra,tp
	LH t1,-82(fp)
	AND t1,t1,t0
	SRA t1,ra,t1
	LD ra,-16(fp)
	LH t2,-78(fp)
	ADDI a0,zero,10
	MUL t2,t2,a0
	ADD ra,ra,t2
	SH t1,0(ra)
	LW ra,-48(fp)
	LD t1,-24(fp)
	ADDI t1,t1,10
	LH t1,0(t1)
	MUL ra,ra,t1
	LW t1,-68(fp)
	ADD ra,ra,t1
	LW t1,-64(fp)
	ADD ra,ra,t1
	SLL ra,ra,tp
	SRA ra,ra,tp
	LH t1,-82(fp)
	AND t1,t1,t0
	SRA t1,ra,t1
	LD ra,-16(fp)
	LH t2,-78(fp)
	ADDI a0,zero,6
	MUL t2,t2,a0
	ADD ra,ra,t2
	SH t1,0(ra)
	LW ra,-44(fp)
	LD t1,-24(fp)
	ADDI t1,t1,14
	LH t1,0(t1)
	MUL ra,ra,t1
	LW t1,-72(fp)
	ADD ra,ra,t1
	LW t1,-60(fp)
	ADD ra,ra,t1
	SLL ra,ra,tp
	SRA ra,ra,tp
	LH tp,-82(fp)
	AND tp,tp,t0
	SRA tp,ra,tp
	LD ra,-16(fp)
	LH t0,-78(fp)
	SLLI t0,t0,1
	ADD ra,ra,t0
	SH tp,0(ra)
	JAL zero,.LBB0_9
.LBB0_9:                                # %for.inc221
                                        #   in Loop: Header=BB0_3 Depth=2
	LH ra,-74(fp)
	ADDI ra,ra,1
	SH ra,-74(fp)
	LH tp,-84(fp)
	LD ra,-16(fp)
	SLLI tp,tp,1
	ADD ra,ra,tp
	SD ra,-16(fp)
	JAL zero,.LBB0_3
.LBB0_10:                               # %for.end224
                                        #   in Loop: Header=BB0_1 Depth=1
	JAL zero,.LBB0_11
.LBB0_11:                               # %for.inc225
                                        #   in Loop: Header=BB0_1 Depth=1
	LH ra,-78(fp)
	ADDI ra,ra,7
	SH ra,-78(fp)
	LH ra,-80(fp)
	ADDI ra,ra,3
	SH ra,-80(fp)
	LH ra,-82(fp)
	ADDI ra,ra,3
	SH ra,-82(fp)
	LH ra,-84(fp)
	ADDI ra,ra,-7
	SH ra,-84(fp)
	LD ra,-16(fp)
	ADDI ra,ra,-128
	SD ra,-16(fp)
	JAL zero,.LBB0_1
.LBB0_12:                               # %for.end239
	LD fp,104(sp)
	ADDI sp,sp,112
	JALR zero,0(ra)
.Lfunc_end0:
	.size	jpegdct, .Lfunc_end0-jpegdct
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
