.text
.file	"libedn_reduced.c"
.globl	jpegdct                         # -- Begin function jpegdct
.type	jpegdct,@function
jpegdct:                                # @jpegdct
# %bb.0:                                # %entry
ADDI X2,X2,3984
SD X8,104(X2)
ADDI X8,X2,112
SD X10,4080(X8)
SD X11,4072(X8)
ADDI X1,X0,1
SH X1,4018(X8)
ADDI X1,X0,0
SH X1,4016(X8)
ADDI X1,X0,13
SH X1,4014(X8)
ADDI X1,X0,8
SH X1,4012(X8)
JAL X0,%lo12(.LBB0_1)
.LBB0_1:                                # %for.cond
                                      # =>This Loop Header: Depth=1
                                      #     Child Loop BB0_3 Depth 2
                                      #       Child Loop BB0_5 Depth 3
LH X4,4018(X8)
ADDI X1,X0,8
BLT X1,X4,.LBB0_12
JAL X0,%lo12(.LBB0_2)
.LBB0_2:                                # %for.body
                                      #   in Loop: Header=BB0_1 Depth=1
ADDI X1,X0,0
SH X1,4022(X8)
JAL X0,%lo12(.LBB0_3)
.LBB0_3:                                # %for.cond2
                                      #   Parent Loop BB0_1 Depth=1
                                      # =>  This Loop Header: Depth=2
                                      #       Child Loop BB0_5 Depth 3
LH X4,4022(X8)
ADDI X1,X0,7
BLT X1,X4,.LBB0_10
JAL X0,%lo12(.LBB0_4)
.LBB0_4:                                # %for.body6
                                      #   in Loop: Header=BB0_3 Depth=2
ADDI X1,X0,0
SH X1,4020(X8)
JAL X0,%lo12(.LBB0_5)
.LBB0_5:                                # %for.cond7
                                      #   Parent Loop BB0_1 Depth=1
                                      #     Parent Loop BB0_3 Depth=2
                                      # =>    This Inner Loop Header: Depth=3
LH X4,4020(X8)
ADDI X1,X0,3
BLT X1,X4,.LBB0_8
JAL X0,%lo12(.LBB0_6)
.LBB0_6:                                # %for.body11
                                      #   in Loop: Header=BB0_5 Depth=3
LD X6,4080(X8)
LH X7,4018(X8)
LH X1,4020(X8)
MUL X4,X7,X1
SLLI X4,X4,1
ADD X4,X6,X4
LH X4,0(X4)
ADDI X5,X0,7
SUB X10,X5,X1
MUL X7,X7,X10
ADDI X10,X0,32
SLL X7,X7,X10
SRA X7,X7,X10
SLLI X7,X7,1
ADD X6,X6,X7
LH X6,0(X6)
ADD X6,X4,X6
SLLI X4,X1,2
ADDI X1,X8,4024
ADD X4,X1,X4
SW X6,0(X4)
LD X6,4080(X8)
LH X7,4018(X8)
LH X11,4020(X8)
MUL X4,X7,X11
SLLI X4,X4,1
ADD X4,X6,X4
LH X4,0(X4)
SUB X5,X5,X11
MUL X7,X7,X5
SLL X7,X7,X10
SRA X7,X7,X10
SLLI X7,X7,1
ADD X6,X6,X7
LH X6,0(X6)
SUB X4,X4,X6
SLLI X5,X5,2
ADD X1,X1,X5
SW X4,0(X1)
JAL X0,%lo12(.LBB0_7)
.LBB0_7:                                # %for.inc
                                      #   in Loop: Header=BB0_5 Depth=3
LH X1,4020(X8)
ADDI X1,X1,1
SH X1,4020(X8)
JAL X0,%lo12(.LBB0_5)
.LBB0_8:                                # %for.end
                                      #   in Loop: Header=BB0_3 Depth=2
LW X1,4024(X8)
LW X4,4036(X8)
ADD X1,X1,X4
SW X1,4056(X8)
LW X1,4024(X8)
LW X4,4036(X8)
SUB X1,X1,X4
SW X1,4060(X8)
LW X1,4028(X8)
LW X4,4032(X8)
ADD X1,X1,X4
SW X1,4064(X8)
LW X1,4028(X8)
LW X4,4032(X8)
SUB X1,X1,X4
SW X1,4068(X8)
LW X1,4056(X8)
LW X4,4064(X8)
	ADD X1,X1,X4
	ADDI X6,X0,32
	SD X6,3996(X8)
	SLL X1,X1,X6
	SRA X1,X1,X6
	LH X4,4016(X8)
	ANDI X4,X4,4095
	SRA X4,X1,X4
	LD X1,4080(X8)
	SH X4,0(X1)
	LW X1,4056(X8)
	LW X4,4064(X8)
	SUB X1,X1,X4
	SLL X1,X1,X6
	SRA X1,X1,X6
	LH X4,4016(X8)
	ANDI X4,X4,4095
	SRA X4,X1,X4
	LD X1,4080(X8)
	LH X5,4018(X8)
	SLLI X5,X5,3
	ADD X1,X1,X5
	SH X4,0(X1)
	LW X1,4068(X8)
	LW X4,4060(X8)
	ADD X1,X1,X4
	ADDI X4,X0,48
	SD X4,4004(X8)
	SLL X1,X1,X4
	SRA X1,X1,X4
	LD X5,4072(X8)
	LH X5,20(X5)
	MUL X1,X1,X5
	SW X1,4056(X8)
	LW X1,4056(X8)
	LW X5,4060(X8)
	LD X7,4072(X8)
	LH X7,18(X7)
	MUL X5,X5,X7
	SLL X5,X5,X6
	SRA X5,X5,X6
	LH X7,4014(X8)
	ANDI X7,X7,4095
	SRA X5,X5,X7
	ADD X5,X1,X5
	LD X1,4080(X8)
	LH X7,4018(X8)
	SLLI X7,X7,2
	ADD X1,X1,X7
	SH X5,0(X1)
	LW X1,4056(X8)
	LW X5,4068(X8)
	LD X7,4072(X8)
	LH X7,22(X7)
	MUL X5,X5,X7
	SLL X5,X5,X6
	SRA X5,X5,X6
	LH X6,4014(X8)
	ANDI X6,X6,4095
	SRA X5,X5,X6
	ADD X5,X1,X5
	LD X1,4080(X8)
	LH X6,4018(X8)
	ADDI X7,X0,12
	MUL X6,X6,X7
	ADD X1,X1,X6
	SH X5,0(X1)
	LW X1,4040(X8)
	LW X5,4052(X8)
	ADD X1,X1,X5
	SLL X1,X1,X4
	SRA X1,X1,X4
	LD X5,4072(X8)
	LH X5,4(X5)
	MUL X1,X1,X5
	SW X1,4024(X8)
	LW X1,4044(X8)
	LW X5,4048(X8)
	ADD X1,X1,X5
	SLL X1,X1,X4
	SRA X1,X1,X4
	LD X5,4072(X8)
	LH X5,0(X5)
	MUL X1,X1,X5
	SW X1,4028(X8)
	LW X1,4040(X8)
	LW X5,4048(X8)
	ADD X1,X1,X5
	SW X1,4032(X8)
	LW X1,4044(X8)
	LW X5,4052(X8)
	ADD X1,X1,X5
	SW X1,4036(X8)
	LW X1,4032(X8)
	LW X5,4036(X8)
	ADD X1,X1,X5
	SLL X1,X1,X4
	SRA X1,X1,X4
	LD X5,4072(X8)
	LH X5,16(X5)
	MUL X1,X1,X5
	SW X1,4056(X8)
	LH X1,4032(X8)
	LD X5,4072(X8)
	LH X5,2(X5)
	MUL X1,X1,X5
	LW X5,4056(X8)
	ADD X1,X1,X5
	SW X1,4032(X8)
	LH X1,4036(X8)
	LD X5,4072(X8)
	LH X5,6(X5)
	MUL X1,X1,X5
	LW X5,4056(X8)
	ADD X1,X1,X5
	SW X1,4036(X8)
	LW X1,4040(X8)
	LD X5,4072(X8)
	ADDI X5,X5,8
	LH X5,0(X5)
	MUL X1,X1,X5
	LW X5,4024(X8)
	ADD X1,X1,X5
	LW X5,4032(X8)
	ADD X1,X1,X5
	SLL X1,X1,X4
	SRA X1,X1,X4
	LH X5,4014(X8)
	ANDI X5,X5,4095
	SRA X5,X1,X5
	LD X1,4080(X8)
	LH X6,4018(X8)
	ADDI X7,X0,14
	MUL X6,X6,X7
	ADD X1,X1,X6
	SH X5,0(X1)
	LW X1,4044(X8)
	LD X5,4072(X8)
	ADDI X5,X5,12
	LH X5,0(X5)
	MUL X1,X1,X5
	LW X5,4028(X8)
	ADD X1,X1,X5
	LW X5,4036(X8)
	ADD X1,X1,X5
	SLL X1,X1,X4
	SRA X1,X1,X4
	LH X5,4014(X8)
	ANDI X5,X5,4095
	SRA X5,X1,X5
	LD X1,4080(X8)
	LH X6,4018(X8)
	ADDI X7,X0,10
	MUL X6,X6,X7
	ADD X1,X1,X6
	SH X5,0(X1)
	LW X1,4048(X8)
	LD X5,4072(X8)
	ADDI X5,X5,10
	LH X5,0(X5)
	MUL X1,X1,X5
	LW X5,4028(X8)
	ADD X1,X1,X5
	LW X5,4032(X8)
	ADD X1,X1,X5
	SLL X1,X1,X4
	SRA X1,X1,X4
	LH X5,4014(X8)
	ANDI X5,X5,4095
	SRA X5,X1,X5
	LD X1,4080(X8)
	LH X6,4018(X8)
	ADDI X7,X0,6
	MUL X6,X6,X7
	ADD X1,X1,X6
	SH X5,0(X1)
	LW X1,4052(X8)
	LD X5,4072(X8)
	ADDI X5,X5,14
	LH X5,0(X5)
	MUL X1,X1,X5
	LW X5,4024(X8)
	ADD X1,X1,X5
	LW X5,4036(X8)
	ADD X1,X1,X5
	SLL X1,X1,X4
	SRA X1,X1,X4
	LH X4,4014(X8)
	ANDI X4,X4,4095
	SRA X4,X1,X4
	LD X1,4080(X8)
	LH X5,4018(X8)
	SLLI X5,X5,1
	ADD X1,X1,X5
	SH X4,0(X1)
	JAL X0,%lo12(.LBB0_9)
.LBB0_9:                                # %for.inc221
                                        #   in Loop: Header=BB0_3 Depth=2
	LH X1,4022(X8)
	ADDI X1,X1,1
	SH X1,4022(X8)
	LH X4,4012(X8)
	LD X1,4080(X8)
	SLLI X4,X4,1
	ADD X1,X1,X4
	SD X1,4080(X8)
	JAL X0,%lo12(.LBB0_3)
.LBB0_10:                               # %for.end224
                                        #   in Loop: Header=BB0_1 Depth=1
	JAL X0,%lo12(.LBB0_11)
.LBB0_11:                               # %for.inc225
                                        #   in Loop: Header=BB0_1 Depth=1
	LH X1,4018(X8)
	ADDI X1,X1,7
	SH X1,4018(X8)
	LH X1,4016(X8)
	ADDI X1,X1,3
	SH X1,4016(X8)
	LH X1,4014(X8)
	ADDI X1,X1,3
	SH X1,4014(X8)
	LH X1,4012(X8)
	ADDI X1,X1,4089
	SH X1,4012(X8)
	LD X1,4080(X8)
	ADDI X1,X1,3968
	SD X1,4080(X8)
	JAL X0,%lo12(.LBB0_1)
.LBB0_12:                               # %for.end239
	LD X8,104(X2)
	ADDI X2,X2,112
	JALR X0,0(X1)
.Lfunc_end0:
	.size	jpegdct, .Lfunc_end0-jpegdct
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig