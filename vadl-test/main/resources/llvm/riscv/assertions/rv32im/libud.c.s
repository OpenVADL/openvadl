	.text
	.file	"libud.c"
	.globl	ludcmp                          # -- Begin function ludcmp
	.type	ludcmp,@function
ludcmp:                                 # @ludcmp
# %bb.0:                                # %entry
	ADDI sp,sp,-432
	SW fp,428(sp)
	SW ra,424(sp)
	ADDI fp,sp,432
	SW a0,-12(fp)
	SW a1,-16(fp)
	ADDI a0,zero,0
	SW a0,-20(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_3 Depth 2
                                        #       Child Loop BB0_6 Depth 3
                                        #     Child Loop BB0_13 Depth 2
                                        #       Child Loop BB0_15 Depth 3
	LW a0,-20(fp)
	LW a1,-16(fp)
	BGE a0,a1,.LBB0_22
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-20(fp)
	ADDI a0,a0,1
	SW a0,-24(fp)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.cond1
                                        #   Parent Loop BB0_1 Depth=1
                                        # =>  This Loop Header: Depth=2
                                        #       Child Loop BB0_6 Depth 3
	LW a1,-24(fp)
	LW a0,-16(fp)
	BLT a0,a1,.LBB0_12
	JAL zero,.LBB0_4
.LBB0_4:                                # %for.body3
                                        #   in Loop: Header=BB0_3 Depth=2
	LW a0,-24(fp)
	ADDI a1,zero,80
	MUL a0,a0,a1
	LW a1,-20(fp)
	SLLI a1,a1,2
	ADD a0,a0,a1
	LUI a1,%hi(a)
	ADDI a1,a1,%lo(a)
	ADD a0,a0,a1
	LW a0,0(a0)
	SW a0,-32(fp)
	LW a0,-20(fp)
	ADDI a1,zero,0
	BEQ a0,a1,.LBB0_10
	JAL zero,.LBB0_5
.LBB0_5:                                # %if.then
                                        #   in Loop: Header=BB0_3 Depth=2
	ADDI a0,zero,0
	SW a0,-28(fp)
	JAL zero,.LBB0_6
.LBB0_6:                                # %for.cond6
                                        #   Parent Loop BB0_1 Depth=1
                                        #     Parent Loop BB0_3 Depth=2
                                        # =>    This Inner Loop Header: Depth=3
	LW a0,-28(fp)
	LW a1,-20(fp)
	BGE a0,a1,.LBB0_9
	JAL zero,.LBB0_7
.LBB0_7:                                # %for.body8
                                        #   in Loop: Header=BB0_6 Depth=3
	LW a0,-24(fp)
	ADDI a3,zero,80
	MUL a0,a0,a3
	LW a1,-28(fp)
	SLLI a2,a1,2
	ADD a0,a0,a2
	LUI a2,%hi(a)
	ADDI a2,a2,%lo(a)
	ADD a0,a0,a2
	LW a0,0(a0)
	MUL a1,a1,a3
	LW a3,-20(fp)
	SLLI a3,a3,2
	ADD a1,a1,a3
	ADD a1,a1,a2
	LW a1,0(a1)
	MUL a1,a0,a1
	LW a0,-32(fp)
	SUB a0,a0,a1
	SW a0,-32(fp)
	JAL zero,.LBB0_8
.LBB0_8:                                # %for.inc
                                        #   in Loop: Header=BB0_6 Depth=3
	LW a0,-28(fp)
	ADDI a0,a0,1
	SW a0,-28(fp)
	JAL zero,.LBB0_6
.LBB0_9:                                # %for.end
                                        #   in Loop: Header=BB0_3 Depth=2
	JAL zero,.LBB0_10
.LBB0_10:                               # %if.end
                                        #   in Loop: Header=BB0_3 Depth=2
	LW a0,-32(fp)
	LW a2,-20(fp)
	ADDI a4,zero,80
	MUL a1,a2,a4
	SLLI a3,a2,2
	ADD a1,a1,a3
	LUI a2,%hi(a)
	ADDI a2,a2,%lo(a)
	ADD a1,a1,a2
	LW a1,0(a1)
	DIV a1,a0,a1
	LW a0,-24(fp)
	MUL a0,a0,a4
	ADD a0,a0,a3
	ADD a0,a0,a2
	SW a1,0(a0)
	JAL zero,.LBB0_11
.LBB0_11:                               # %for.inc17
                                        #   in Loop: Header=BB0_3 Depth=2
	LW a0,-24(fp)
	ADDI a0,a0,1
	SW a0,-24(fp)
	JAL zero,.LBB0_3
.LBB0_12:                               # %for.end19
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-20(fp)
	ADDI a0,a0,1
	SW a0,-24(fp)
	JAL zero,.LBB0_13
.LBB0_13:                               # %for.cond21
                                        #   Parent Loop BB0_1 Depth=1
                                        # =>  This Loop Header: Depth=2
                                        #       Child Loop BB0_15 Depth 3
	LW a1,-24(fp)
	LW a0,-16(fp)
	BLT a0,a1,.LBB0_20
	JAL zero,.LBB0_14
.LBB0_14:                               # %for.body23
                                        #   in Loop: Header=BB0_13 Depth=2
	LW a0,-20(fp)
	ADDI a1,zero,80
	MUL a0,a0,a1
	LW a1,-24(fp)
	SLLI a1,a1,2
	ADD a1,a0,a1
	LUI a0,%hi(a)
	ADDI a0,a0,%lo(a)
	ADD a0,a0,a1
	LW a0,80(a0)
	SW a0,-32(fp)
	ADDI a0,zero,0
	SW a0,-28(fp)
	JAL zero,.LBB0_15
.LBB0_15:                               # %for.cond27
                                        #   Parent Loop BB0_1 Depth=1
                                        #     Parent Loop BB0_13 Depth=2
                                        # =>    This Inner Loop Header: Depth=3
	LW a1,-28(fp)
	LW a0,-20(fp)
	BLT a0,a1,.LBB0_18
	JAL zero,.LBB0_16
.LBB0_16:                               # %for.body29
                                        #   in Loop: Header=BB0_15 Depth=3
	LW a0,-20(fp)
	ADDI a3,zero,80
	MUL a0,a0,a3
	LW a1,-28(fp)
	SLLI a2,a1,2
	ADD a0,a0,a2
	LUI a2,%hi(a)
	ADDI a2,a2,%lo(a)
	ADD a0,a2,a0
	LW a0,80(a0)
	MUL a1,a1,a3
	LW a3,-24(fp)
	SLLI a3,a3,2
	ADD a1,a1,a3
	ADD a1,a1,a2
	LW a1,0(a1)
	MUL a1,a0,a1
	LW a0,-32(fp)
	SUB a0,a0,a1
	SW a0,-32(fp)
	JAL zero,.LBB0_17
.LBB0_17:                               # %for.inc37
                                        #   in Loop: Header=BB0_15 Depth=3
	LW a0,-28(fp)
	ADDI a0,a0,1
	SW a0,-28(fp)
	JAL zero,.LBB0_15
.LBB0_18:                               # %for.end39
                                        #   in Loop: Header=BB0_13 Depth=2
	LW a1,-32(fp)
	LW a0,-20(fp)
	ADDI a2,zero,80
	MUL a0,a0,a2
	LW a2,-24(fp)
	SLLI a2,a2,2
	ADD a2,a0,a2
	LUI a0,%hi(a)
	ADDI a0,a0,%lo(a)
	ADD a0,a0,a2
	SW a1,80(a0)
	JAL zero,.LBB0_19
.LBB0_19:                               # %for.inc43
                                        #   in Loop: Header=BB0_13 Depth=2
	LW a0,-24(fp)
	ADDI a0,a0,1
	SW a0,-24(fp)
	JAL zero,.LBB0_13
.LBB0_20:                               # %for.end45
                                        #   in Loop: Header=BB0_1 Depth=1
	JAL zero,.LBB0_21
.LBB0_21:                               # %for.inc46
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-20(fp)
	ADDI a0,a0,1
	SW a0,-20(fp)
	JAL zero,.LBB0_1
.LBB0_22:                               # %for.end48
	LUI a0,%hi(b)
	ADDI a0,a0,%lo(b)
	LW a0,0(a0)
	SW a0,-432(fp)
	ADDI a0,zero,1
	SW a0,-20(fp)
	JAL zero,.LBB0_23
.LBB0_23:                               # %for.cond50
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_25 Depth 2
	LW a1,-20(fp)
	LW a0,-16(fp)
	BLT a0,a1,.LBB0_30
	JAL zero,.LBB0_24
.LBB0_24:                               # %for.body52
                                        #   in Loop: Header=BB0_23 Depth=1
	LW a0,-20(fp)
	SLLI a0,a0,2
	LUI a1,%hi(b)
	ADDI a1,a1,%lo(b)
	ADD a0,a0,a1
	LW a0,0(a0)
	SW a0,-32(fp)
	ADDI a0,zero,0
	SW a0,-24(fp)
	JAL zero,.LBB0_25
.LBB0_25:                               # %for.cond54
                                        #   Parent Loop BB0_23 Depth=1
                                        # =>  This Inner Loop Header: Depth=2
	LW a0,-24(fp)
	LW a1,-20(fp)
	BGE a0,a1,.LBB0_28
	JAL zero,.LBB0_26
.LBB0_26:                               # %for.body56
                                        #   in Loop: Header=BB0_25 Depth=2
	LW a0,-20(fp)
	ADDI a1,zero,80
	MUL a0,a0,a1
	LW a1,-24(fp)
	SLLI a2,a1,2
	ADD a0,a0,a2
	LUI a1,%hi(a)
	ADDI a1,a1,%lo(a)
	ADD a0,a0,a1
	LW a0,0(a0)
	ADDI a1,fp,-432
	ADD a1,a1,a2
	LW a1,0(a1)
	MUL a1,a0,a1
	LW a0,-32(fp)
	SUB a0,a0,a1
	SW a0,-32(fp)
	JAL zero,.LBB0_27
.LBB0_27:                               # %for.inc62
                                        #   in Loop: Header=BB0_25 Depth=2
	LW a0,-24(fp)
	ADDI a0,a0,1
	SW a0,-24(fp)
	JAL zero,.LBB0_25
.LBB0_28:                               # %for.end64
                                        #   in Loop: Header=BB0_23 Depth=1
	LW a1,-32(fp)
	LW a0,-20(fp)
	SLLI a2,a0,2
	ADDI a0,fp,-432
	ADD a0,a0,a2
	SW a1,0(a0)
	JAL zero,.LBB0_29
.LBB0_29:                               # %for.inc66
                                        #   in Loop: Header=BB0_23 Depth=1
	LW a0,-20(fp)
	ADDI a0,a0,1
	SW a0,-20(fp)
	JAL zero,.LBB0_23
.LBB0_30:                               # %for.end68
	LW a2,-16(fp)
	SLLI a0,a2,2
	ADDI a1,fp,-432
	ADD a1,a1,a0
	LW a1,0(a1)
	ADDI a3,zero,80
	MUL a2,a2,a3
	ADD a2,a2,a0
	LUI a3,%hi(a)
	ADDI a3,a3,%lo(a)
	ADD a2,a2,a3
	LW a2,0(a2)
	DIV a1,a1,a2
	LUI a2,%hi(x)
	ADDI a2,a2,%lo(x)
	ADD a0,a0,a2
	SW a1,0(a0)
	LW a0,-16(fp)
	ADDI a0,a0,-1
	SW a0,-20(fp)
	JAL zero,.LBB0_31
.LBB0_31:                               # %for.cond75
                                        # =>This Loop Header: Depth=1
                                        #     Child Loop BB0_33 Depth 2
	LW a0,-20(fp)
	ADDI a1,zero,0
	BLT a0,a1,.LBB0_38
	JAL zero,.LBB0_32
.LBB0_32:                               # %for.body77
                                        #   in Loop: Header=BB0_31 Depth=1
	LW a0,-20(fp)
	SLLI a1,a0,2
	ADDI a0,fp,-432
	ADD a0,a0,a1
	LW a0,0(a0)
	SW a0,-32(fp)
	LW a0,-20(fp)
	ADDI a0,a0,1
	SW a0,-24(fp)
	JAL zero,.LBB0_33
.LBB0_33:                               # %for.cond80
                                        #   Parent Loop BB0_31 Depth=1
                                        # =>  This Inner Loop Header: Depth=2
	LW a1,-24(fp)
	LW a0,-16(fp)
	BLT a0,a1,.LBB0_36
	JAL zero,.LBB0_34
.LBB0_34:                               # %for.body82
                                        #   in Loop: Header=BB0_33 Depth=2
	LW a0,-20(fp)
	ADDI a1,zero,80
	MUL a0,a0,a1
	LW a1,-24(fp)
	SLLI a1,a1,2
	ADD a0,a0,a1
	LUI a2,%hi(a)
	ADDI a2,a2,%lo(a)
	ADD a0,a0,a2
	LW a0,0(a0)
	LUI a2,%hi(x)
	ADDI a2,a2,%lo(x)
	ADD a1,a1,a2
	LW a1,0(a1)
	MUL a1,a0,a1
	LW a0,-32(fp)
	SUB a0,a0,a1
	SW a0,-32(fp)
	JAL zero,.LBB0_35
.LBB0_35:                               # %for.inc88
                                        #   in Loop: Header=BB0_33 Depth=2
	LW a0,-24(fp)
	ADDI a0,a0,1
	SW a0,-24(fp)
	JAL zero,.LBB0_33
.LBB0_36:                               # %for.end90
                                        #   in Loop: Header=BB0_31 Depth=1
	LW a1,-32(fp)
	LW a0,-20(fp)
	ADDI a2,zero,80
	MUL a2,a0,a2
	SLLI a0,a0,2
	ADD a2,a2,a0
	LUI a3,%hi(a)
	ADDI a3,a3,%lo(a)
	ADD a2,a2,a3
	LW a2,0(a2)
	DIV a1,a1,a2
	LUI a2,%hi(x)
	ADDI a2,a2,%lo(x)
	ADD a0,a0,a2
	SW a1,0(a0)
	JAL zero,.LBB0_37
.LBB0_37:                               # %for.inc95
                                        #   in Loop: Header=BB0_31 Depth=1
	LW a0,-20(fp)
	ADDI a0,a0,-1
	SW a0,-20(fp)
	JAL zero,.LBB0_31
.LBB0_38:                               # %for.end96
	ADDI a0,zero,0
	LW ra,424(sp)
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

	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym a
	.addrsig_sym b
	.addrsig_sym x
