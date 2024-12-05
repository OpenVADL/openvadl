	.text
	.file	"libud.c"
	.globl	ludcmp                          # -- Begin function ludcmp
	.type	ludcmp,@function
ludcmp:                                 # @ludcmp
# %bb.0:                                # %entry
	ADDI sp,sp,-432
	SW fp,428(sp)
	ADDI fp,sp,432
	SW a0,-8(fp)
	SW a1,-12(fp)
	ADDI ra,zero,0
	SW ra,-16(fp)
	JAL zero,%hi(.LBB0_1)
.LBB0_1:                                # %for.cond
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_3 Depth 2
                                        #       Child Loop BB0_6 Depth 3
                                        #     Child Loop BB0_13 Depth 2
                                        #       Child Loop BB0_15 Depth 3
	LW ra,-16(fp)
	LW tp,-12(fp)
	BGE ra,tp,.LBB0_22
	JAL zero,%hi(.LBB0_2)
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	LW ra,-16(fp)
	ADDI ra,ra,1
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_3)
.LBB0_3:                                # %for.cond1
                                        #   Parent Loop BB0_1 Depth=1
                                        # =>  This Loop Header: Depth=2
                                        #       Child Loop BB0_6 Depth 3
	LW tp,-20(fp)
	LW ra,-12(fp)
	BLT ra,tp,.LBB0_12
	JAL zero,%hi(.LBB0_4)
.LBB0_4:                                # %for.body3
                                        #   in Loop: Header=BB0_3 Depth=2
	LW ra,-20(fp)
	ADDI tp,zero,80
	MUL ra,ra,tp
	LW tp,-16(fp)
	SLLI tp,tp,2
	ADD ra,ra,tp
	LUI tp,%hi(a)
	ADDI tp,tp,%lo(a)
	ADD ra,ra,tp
	LW ra,0(ra)
	SW ra,-28(fp)
	LW ra,-16(fp)
	ADDI tp,zero,0
	BEQ ra,tp,.LBB0_10
	JAL zero,%hi(.LBB0_5)
.LBB0_5:                                # %if.then
                                        #   in Loop: Header=BB0_3 Depth=2
	ADDI ra,zero,0
	SW ra,-24(fp)
	JAL zero,%hi(.LBB0_6)
.LBB0_6:                                # %for.cond6
                                        #   Parent Loop BB0_1 Depth=1
                                        #     Parent Loop BB0_3 Depth=2
                                        # =>    This Inner Loop Header: Depth=3
	LW ra,-24(fp)
	LW tp,-16(fp)
	BGE ra,tp,.LBB0_9
	JAL zero,%hi(.LBB0_7)
.LBB0_7:                                # %for.body8
                                        #   in Loop: Header=BB0_6 Depth=3
	LW ra,-20(fp)
	ADDI X6,zero,80
	MUL ra,ra,X6
	LW tp,-24(fp)
	SLLI X5,tp,2
	ADD ra,ra,X5
	LUI X5,%hi(a)
	ADDI X5,X5,%lo(a)
	ADD ra,ra,X5
	LW ra,0(ra)
	MUL tp,tp,X6
	LW X6,-16(fp)
	SLLI X6,X6,2
	ADD tp,tp,X6
	ADD tp,tp,X5
	LW tp,0(tp)
	MUL tp,ra,tp
	LW ra,-28(fp)
	SUB ra,ra,tp
	SW ra,-28(fp)
	JAL zero,%hi(.LBB0_8)
.LBB0_8:                                # %for.inc
                                        #   in Loop: Header=BB0_6 Depth=3
	LW ra,-24(fp)
	ADDI ra,ra,1
	SW ra,-24(fp)
	JAL zero,%hi(.LBB0_6)
.LBB0_9:                                # %for.end
                                        #   in Loop: Header=BB0_3 Depth=2
	JAL zero,%hi(.LBB0_10)
.LBB0_10:                               # %if.end
                                        #   in Loop: Header=BB0_3 Depth=2
	LW ra,-28(fp)
	LW X5,-16(fp)
	ADDI X7,zero,80
	MUL tp,X5,X7
	SLLI X6,X5,2
	ADD tp,tp,X6
	LUI X5,%hi(a)
	ADDI X5,X5,%lo(a)
	ADD tp,tp,X5
	LW tp,0(tp)
	DIV tp,ra,tp
	LW ra,-20(fp)
	MUL ra,ra,X7
	ADD ra,ra,X6
	ADD ra,ra,X5
	SW tp,0(ra)
	JAL zero,%hi(.LBB0_11)
.LBB0_11:                               # %for.inc17
                                        #   in Loop: Header=BB0_3 Depth=2
	LW ra,-20(fp)
	ADDI ra,ra,1
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_3)
.LBB0_12:                               # %for.end19
                                        #   in Loop: Header=BB0_1 Depth=1
	LW ra,-16(fp)
	ADDI ra,ra,1
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_13)
.LBB0_13:                               # %for.cond21
                                        #   Parent Loop BB0_1 Depth=1
                                        # =>  This Loop Header: Depth=2
                                        #       Child Loop BB0_15 Depth 3
	LW tp,-20(fp)
	LW ra,-12(fp)
	BLT ra,tp,.LBB0_20
	JAL zero,%hi(.LBB0_14)
.LBB0_14:                               # %for.body23
                                        #   in Loop: Header=BB0_13 Depth=2
	LW ra,-16(fp)
	ADDI tp,zero,80
	MUL ra,ra,tp
	LW tp,-20(fp)
	SLLI tp,tp,2
	ADD tp,ra,tp
	LUI ra,%hi(a)
	ADDI ra,ra,%lo(a)
	ADD ra,ra,tp
	LW ra,80(ra)
	SW ra,-28(fp)
	ADDI ra,zero,0
	SW ra,-24(fp)
	JAL zero,%hi(.LBB0_15)
.LBB0_15:                               # %for.cond27
                                        #   Parent Loop BB0_1 Depth=1
                                        #     Parent Loop BB0_13 Depth=2
                                        # =>    This Inner Loop Header: Depth=3
	LW tp,-24(fp)
	LW ra,-16(fp)
	BLT ra,tp,.LBB0_18
	JAL zero,%hi(.LBB0_16)
.LBB0_16:                               # %for.body29
                                        #   in Loop: Header=BB0_15 Depth=3
	LW ra,-16(fp)
	ADDI X6,zero,80
	MUL ra,ra,X6
	LW tp,-24(fp)
	SLLI X5,tp,2
	ADD ra,ra,X5
	LUI X5,%hi(a)
	ADDI X5,X5,%lo(a)
	ADD ra,X5,ra
	LW ra,80(ra)
	MUL tp,tp,X6
	LW X6,-20(fp)
	SLLI X6,X6,2
	ADD tp,tp,X6
	ADD tp,tp,X5
	LW tp,0(tp)
	MUL tp,ra,tp
	LW ra,-28(fp)
	SUB ra,ra,tp
	SW ra,-28(fp)
	JAL zero,%hi(.LBB0_17)
.LBB0_17:                               # %for.inc37
                                        #   in Loop: Header=BB0_15 Depth=3
	LW ra,-24(fp)
	ADDI ra,ra,1
	SW ra,-24(fp)
	JAL zero,%hi(.LBB0_15)
.LBB0_18:                               # %for.end39
                                        #   in Loop: Header=BB0_13 Depth=2
	LW tp,-28(fp)
	LW ra,-16(fp)
	ADDI X5,zero,80
	MUL ra,ra,X5
	LW X5,-20(fp)
	SLLI X5,X5,2
	ADD X5,ra,X5
	LUI ra,%hi(a)
	ADDI ra,ra,%lo(a)
	ADD ra,ra,X5
	SW tp,80(ra)
	JAL zero,%hi(.LBB0_19)
.LBB0_19:                               # %for.inc43
                                        #   in Loop: Header=BB0_13 Depth=2
	LW ra,-20(fp)
	ADDI ra,ra,1
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_13)
.LBB0_20:                               # %for.end45
                                        #   in Loop: Header=BB0_1 Depth=1
	JAL zero,%hi(.LBB0_21)
.LBB0_21:                               # %for.inc46
                                        #   in Loop: Header=BB0_1 Depth=1
	LW ra,-16(fp)
	ADDI ra,ra,1
	SW ra,-16(fp)
	JAL zero,%hi(.LBB0_1)
.LBB0_22:                               # %for.end48
	LUI ra,%hi(b)
	ADDI ra,ra,%lo(b)
	LW ra,0(ra)
	SW ra,-428(fp)
	ADDI ra,zero,1
	SW ra,-16(fp)
	JAL zero,%hi(.LBB0_23)
.LBB0_23:                               # %for.cond50
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_25 Depth 2
	LW tp,-16(fp)
	LW ra,-12(fp)
	BLT ra,tp,.LBB0_30
	JAL zero,%hi(.LBB0_24)
.LBB0_24:                               # %for.body52
                                        #   in Loop: Header=BB0_23 Depth=1
	LW ra,-16(fp)
	SLLI ra,ra,2
	LUI tp,%hi(b)
	ADDI tp,tp,%lo(b)
	ADD ra,ra,tp
	LW ra,0(ra)
	SW ra,-28(fp)
	ADDI ra,zero,0
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_25)
.LBB0_25:                               # %for.cond54
                                        #   Parent Loop BB0_23 Depth=1
                                        # =>  This Inner Loop Header: Depth=2
	LW ra,-20(fp)
	LW tp,-16(fp)
	BGE ra,tp,.LBB0_28
	JAL zero,%hi(.LBB0_26)
.LBB0_26:                               # %for.body56
                                        #   in Loop: Header=BB0_25 Depth=2
	LW ra,-16(fp)
	ADDI tp,zero,80
	MUL ra,ra,tp
	LW tp,-20(fp)
	SLLI X5,tp,2
	ADD ra,ra,X5
	LUI tp,%hi(a)
	ADDI tp,tp,%lo(a)
	ADD ra,ra,tp
	LW ra,0(ra)
	ADDI tp,fp,-428
	ADD tp,tp,X5
	LW tp,0(tp)
	MUL tp,ra,tp
	LW ra,-28(fp)
	SUB ra,ra,tp
	SW ra,-28(fp)
	JAL zero,%hi(.LBB0_27)
.LBB0_27:                               # %for.inc62
                                        #   in Loop: Header=BB0_25 Depth=2
	LW ra,-20(fp)
	ADDI ra,ra,1
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_25)
.LBB0_28:                               # %for.end64
                                        #   in Loop: Header=BB0_23 Depth=1
	LW tp,-28(fp)
	LW ra,-16(fp)
	SLLI X5,ra,2
	ADDI ra,fp,-428
	ADD ra,ra,X5
	SW tp,0(ra)
	JAL zero,%hi(.LBB0_29)
.LBB0_29:                               # %for.inc66
                                        #   in Loop: Header=BB0_23 Depth=1
	LW ra,-16(fp)
	ADDI ra,ra,1
	SW ra,-16(fp)
	JAL zero,%hi(.LBB0_23)
.LBB0_30:                               # %for.end68
	LW X5,-12(fp)
	SLLI ra,X5,2
	ADDI tp,fp,-428
	ADD tp,tp,ra
	LW tp,0(tp)
	ADDI X6,zero,80
	MUL X5,X5,X6
	ADD X5,X5,ra
	LUI X6,%hi(a)
	ADDI X6,X6,%lo(a)
	ADD X5,X5,X6
	LW X5,0(X5)
	DIV tp,tp,X5
	LUI X5,%hi(x)
	ADDI X5,X5,%lo(x)
	ADD ra,ra,X5
	SW tp,0(ra)
	LW ra,-12(fp)
	ADDI ra,ra,-1
	SW ra,-16(fp)
	JAL zero,%hi(.LBB0_31)
.LBB0_31:                               # %for.cond75
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_33 Depth 2
	LW ra,-16(fp)
	ADDI tp,zero,0
	BLT ra,tp,.LBB0_38
	JAL zero,%hi(.LBB0_32)
.LBB0_32:                               # %for.body77
                                        #   in Loop: Header=BB0_31 Depth=1
	LW ra,-16(fp)
	SLLI tp,ra,2
	ADDI ra,fp,-428
	ADD ra,ra,tp
	LW ra,0(ra)
	SW ra,-28(fp)
	LW ra,-16(fp)
	ADDI ra,ra,1
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_33)
.LBB0_33:                               # %for.cond80
                                        #   Parent Loop BB0_31 Depth=1
                                        # =>  This Inner Loop Header: Depth=2
	LW tp,-20(fp)
	LW ra,-12(fp)
	BLT ra,tp,.LBB0_36
	JAL zero,%hi(.LBB0_34)
.LBB0_34:                               # %for.body82
                                        #   in Loop: Header=BB0_33 Depth=2
	LW ra,-16(fp)
	ADDI tp,zero,80
	MUL ra,ra,tp
	LW tp,-20(fp)
	SLLI tp,tp,2
	ADD ra,ra,tp
	LUI X5,%hi(a)
	ADDI X5,X5,%lo(a)
	ADD ra,ra,X5
	LW ra,0(ra)
	LUI X5,%hi(x)
	ADDI X5,X5,%lo(x)
	ADD tp,tp,X5
	LW tp,0(tp)
	MUL tp,ra,tp
	LW ra,-28(fp)
	SUB ra,ra,tp
	SW ra,-28(fp)
	JAL zero,%hi(.LBB0_35)
.LBB0_35:                               # %for.inc88
                                        #   in Loop: Header=BB0_33 Depth=2
	LW ra,-20(fp)
	ADDI ra,ra,1
	SW ra,-20(fp)
	JAL zero,%hi(.LBB0_33)
.LBB0_36:                               # %for.end90
                                        #   in Loop: Header=BB0_31 Depth=1
	LW tp,-28(fp)
	LW ra,-16(fp)
	ADDI X5,zero,80
	MUL X5,ra,X5
	SLLI ra,ra,2
	ADD X5,X5,ra
	LUI X6,%hi(a)
	ADDI X6,X6,%lo(a)
	ADD X5,X5,X6
	LW X5,0(X5)
	DIV tp,tp,X5
	LUI X5,%hi(x)
	ADDI X5,X5,%lo(x)
	ADD ra,ra,X5
	SW tp,0(ra)
	JAL zero,%hi(.LBB0_37)
.LBB0_37:                               # %for.inc95
                                        #   in Loop: Header=BB0_31 Depth=1
	LW ra,-16(fp)
	ADDI ra,ra,-1
	SW ra,-16(fp)
	JAL zero,%hi(.LBB0_31)
.LBB0_38:                               # %for.end96
	ADDI a0,zero,0
	LW fp,428(sp)
	ADDI sp,sp,432
	JALR zero,0(ra)
.Lfunc_end0:
	.size	ludcmp, .Lfunc_end0-ludcmp
                                        # -- End function
	.type	a,@object                       # @a
	.bss
	.globl	a
	.p2align	2, 0x0
a:
	.zero	1600
	.size	a, 1600

	.type	b,@object                       # @b
	.globl	b
	.p2align	2, 0x0
b:
	.zero	80
	.size	b, 80

	.type	x,@object                       # @x
	.globl	x
	.p2align	2, 0x0
x:
	.zero	80
	.size	x, 80

	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym a
	.addrsig_sym b
	.addrsig_sym x
