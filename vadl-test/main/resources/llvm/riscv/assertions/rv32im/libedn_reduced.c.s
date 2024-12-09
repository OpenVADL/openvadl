	.text
	.file	"libedn_reduced.c"
	.globl	jpegdct                         # -- Begin function jpegdct
	.type	jpegdct,@function
jpegdct:                                # @jpegdct
# %bb.0:                                # %entry
	ADDI sp,sp,-80
	SW fp,76(sp)
	ADDI fp,sp,80
	SW a0,-8(fp)
	SW a1,-12(fp)
	ADDI ra,zero,1
	SH ra,-66(fp)
	ADDI ra,zero,0
	SH ra,-68(fp)
	ADDI ra,zero,13
	SH ra,-70(fp)
	ADDI ra,zero,8
	SH ra,-72(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_3 Depth 2
                                        #       Child Loop BB0_5 Depth 3
	LH tp,-66(fp)
	ADDI ra,zero,8
	BLT ra,tp,.LBB0_12
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	ADDI ra,zero,0
	SH ra,-62(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.cond2
                                        #   Parent Loop BB0_1 Depth=1
                                        # =>  This Loop Header: Depth=2
                                        #       Child Loop BB0_5 Depth 3
	LH tp,-62(fp)
	ADDI ra,zero,7
	BLT ra,tp,.LBB0_10
	JAL zero,.LBB0_4
.LBB0_4:                                # %for.body6
                                        #   in Loop: Header=BB0_3 Depth=2
	ADDI ra,zero,0
	SH ra,-64(fp)
	JAL zero,.LBB0_5
.LBB0_5:                                # %for.cond7
                                        #   Parent Loop BB0_1 Depth=1
                                        #     Parent Loop BB0_3 Depth=2
                                        # =>    This Inner Loop Header: Depth=3
	LH tp,-64(fp)
	ADDI ra,zero,3
	BLT ra,tp,.LBB0_8
	JAL zero,.LBB0_6
.LBB0_6:                                # %for.body11
                                        #   in Loop: Header=BB0_5 Depth=3
	LW t1,-8(fp)
	LH t2,-66(fp)
	LH ra,-64(fp)
	MUL tp,t2,ra
	SLLI tp,tp,1
	ADD tp,t1,tp
	LH tp,0(tp)
	ADDI t0,zero,7
	SUB a0,t0,ra
	MUL t2,t2,a0
	SLLI t2,t2,1
	ADD t1,t1,t2
	LH t1,0(t1)
	ADD t1,tp,t1
	SLLI tp,ra,2
	ADDI ra,fp,-60
	ADD tp,ra,tp
	SW t1,0(tp)
	LW t1,-8(fp)
	LH t2,-66(fp)
	LH a0,-64(fp)
	MUL tp,t2,a0
	SLLI tp,tp,1
	ADD tp,t1,tp
	LH tp,0(tp)
	SUB t0,t0,a0
	MUL t2,t2,t0
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
	LH ra,-64(fp)
	ADDI ra,ra,1
	SH ra,-64(fp)
	JAL zero,.LBB0_5
.LBB0_8:                                # %for.end
                                        #   in Loop: Header=BB0_3 Depth=2
	LW ra,-60(fp)
	LW tp,-48(fp)
	ADD ra,ra,tp
	SW ra,-28(fp)
	LW ra,-60(fp)
	LW tp,-48(fp)
	SUB ra,ra,tp
	SW ra,-24(fp)
	LW ra,-56(fp)
	LW tp,-52(fp)
	ADD ra,ra,tp
	SW ra,-20(fp)
	LW ra,-56(fp)
	LW tp,-52(fp)
	SUB ra,ra,tp
	SW ra,-16(fp)
	LW ra,-28(fp)
	LW tp,-20(fp)
	ADD ra,ra,tp
	LH tp,-68(fp)
	SRA tp,ra,tp
	LW ra,-8(fp)
	SH tp,0(ra)
	LW ra,-28(fp)
	LW tp,-20(fp)
	SUB ra,ra,tp
	LH tp,-68(fp)
	SRA tp,ra,tp
	LW ra,-8(fp)
	LH t0,-66(fp)
	SLLI t0,t0,3
	ADD ra,ra,t0
	SH tp,0(ra)
	LW ra,-16(fp)
	LW tp,-24(fp)
	ADD ra,ra,tp
	ADDI tp,zero,16
	SW tp,-76(fp)
	SLL ra,ra,tp
	SRA ra,ra,tp
	LW t0,-12(fp)
	LH t0,20(t0)
	MUL ra,ra,t0
	SW ra,-28(fp)
	LW ra,-28(fp)
	LW t0,-24(fp)
	LW t1,-12(fp)
	LH t1,18(t1)
	MUL t0,t0,t1
	LH t1,-70(fp)
	SRA t0,t0,t1
	ADD t0,ra,t0
	LW ra,-8(fp)
	LH t1,-66(fp)
	SLLI t1,t1,2
	ADD ra,ra,t1
	SH t0,0(ra)
	LW ra,-28(fp)
	LW t0,-16(fp)
	LW t1,-12(fp)
	LH t1,22(t1)
	MUL t0,t0,t1
	LH t1,-70(fp)
	SRA t0,t0,t1
	ADD t0,ra,t0
	LW ra,-8(fp)
	LH t1,-66(fp)
	ADDI t2,zero,12
	MUL t1,t1,t2
	ADD ra,ra,t1
	SH t0,0(ra)
	LW ra,-44(fp)
	LW t0,-32(fp)
	ADD ra,ra,t0
	SLL ra,ra,tp
	SRA ra,ra,tp
	LW t0,-12(fp)
	LH t0,4(t0)
	MUL ra,ra,t0
	SW ra,-60(fp)
	LW ra,-40(fp)
	LW t0,-36(fp)
	ADD ra,ra,t0
	SLL ra,ra,tp
	SRA ra,ra,tp
	LW t0,-12(fp)
	LH t0,0(t0)
	MUL ra,ra,t0
	SW ra,-56(fp)
	LW ra,-44(fp)
	LW t0,-36(fp)
	ADD ra,ra,t0
	SW ra,-52(fp)
	LW ra,-40(fp)
	LW t0,-32(fp)
	ADD ra,ra,t0
	SW ra,-48(fp)
	LW ra,-52(fp)
	LW t0,-48(fp)
	ADD ra,ra,t0
	SLL ra,ra,tp
	SRA ra,ra,tp
	LW t0,-12(fp)
	LH t0,16(t0)
	MUL ra,ra,t0
	SW ra,-28(fp)
	LH ra,-52(fp)
	LW t0,-12(fp)
	LH t0,2(t0)
	MUL ra,ra,t0
	LW t0,-28(fp)
	ADD ra,ra,t0
	SW ra,-52(fp)
	LH ra,-48(fp)
	LW t0,-12(fp)
	LH t0,6(t0)
	MUL ra,ra,t0
	LW t0,-28(fp)
	ADD ra,ra,t0
	SW ra,-48(fp)
	LW ra,-44(fp)
	LW t0,-12(fp)
	ADDI t0,t0,8
	LH t0,0(t0)
	MUL ra,ra,t0
	LW t0,-60(fp)
	ADD ra,ra,t0
	LW t0,-52(fp)
	ADD ra,ra,t0
	SLL ra,ra,tp
	SRA ra,ra,tp
	LH t0,-70(fp)
	SRA t0,ra,t0
	LW ra,-8(fp)
	LH t1,-66(fp)
	ADDI t2,zero,14
	MUL t1,t1,t2
	ADD ra,ra,t1
	SH t0,0(ra)
	LW ra,-40(fp)
	LW t0,-12(fp)
	ADDI t0,t0,12
	LH t0,0(t0)
	MUL ra,ra,t0
	LW t0,-56(fp)
	ADD ra,ra,t0
	LW t0,-48(fp)
	ADD ra,ra,t0
	SLL ra,ra,tp
	SRA ra,ra,tp
	LH t0,-70(fp)
	SRA t0,ra,t0
	LW ra,-8(fp)
	LH t1,-66(fp)
	ADDI t2,zero,10
	MUL t1,t1,t2
	ADD ra,ra,t1
	SH t0,0(ra)
	LW ra,-36(fp)
	LW t0,-12(fp)
	ADDI t0,t0,10
	LH t0,0(t0)
	MUL ra,ra,t0
	LW t0,-56(fp)
	ADD ra,ra,t0
	LW t0,-52(fp)
	ADD ra,ra,t0
	SLL ra,ra,tp
	SRA ra,ra,tp
	LH t0,-70(fp)
	SRA t0,ra,t0
	LW ra,-8(fp)
	LH t1,-66(fp)
	ADDI t2,zero,6
	MUL t1,t1,t2
	ADD ra,ra,t1
	SH t0,0(ra)
	LW ra,-32(fp)
	LW t0,-12(fp)
	ADDI t0,t0,14
	LH t0,0(t0)
	MUL ra,ra,t0
	LW t0,-60(fp)
	ADD ra,ra,t0
	LW t0,-48(fp)
	ADD ra,ra,t0
	SLL ra,ra,tp
	SRA ra,ra,tp
	LH tp,-70(fp)
	SRA tp,ra,tp
	LW ra,-8(fp)
	LH t0,-66(fp)
	SLLI t0,t0,1
	ADD ra,ra,t0
	SH tp,0(ra)
	JAL zero,.LBB0_9
.LBB0_9:                                # %for.inc221
                                        #   in Loop: Header=BB0_3 Depth=2
	LH ra,-62(fp)
	ADDI ra,ra,1
	SH ra,-62(fp)
	LH tp,-72(fp)
	LW ra,-8(fp)
	SLLI tp,tp,1
	ADD ra,ra,tp
	SW ra,-8(fp)
	JAL zero,.LBB0_3
.LBB0_10:                               # %for.end224
                                        #   in Loop: Header=BB0_1 Depth=1
	JAL zero,.LBB0_11
.LBB0_11:                               # %for.inc225
                                        #   in Loop: Header=BB0_1 Depth=1
	LH ra,-66(fp)
	ADDI ra,ra,7
	SH ra,-66(fp)
	LH ra,-68(fp)
	ADDI ra,ra,3
	SH ra,-68(fp)
	LH ra,-70(fp)
	ADDI ra,ra,3
	SH ra,-70(fp)
	LH ra,-72(fp)
	ADDI ra,ra,-7
	SH ra,-72(fp)
	LW ra,-8(fp)
	ADDI ra,ra,-128
	SW ra,-8(fp)
	JAL zero,.LBB0_1
.LBB0_12:                               # %for.end239
	LW fp,76(sp)
	ADDI sp,sp,80
	JALR zero,0(ra)
.Lfunc_end0:
	.size	jpegdct, .Lfunc_end0-jpegdct
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
