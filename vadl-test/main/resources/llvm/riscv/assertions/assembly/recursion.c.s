	.text
	.file	"recursion.c"
	.globl	recursion                       # -- Begin function recursion
	.type	recursion,@function
recursion:                              # @recursion
# %bb.0:                                # %entry
	ADDI X2,X2,4064
	SD X2,24(X2)
	SD X8,16(X2)
	ADDI X8,X2,32
                                        # kill: def $x1 killed $x10
	SW X10,4072(X8)
	LW X1,4072(X8)
	ADDI X4,X0,1
	BLT X1,X4,.LBB0_2
	JAL X0,%lo12(.LBB0_1)
.LBB0_1:                                # %if.then
	LW X1,4072(X8)
	SW X1,4076(X8)
	JAL X0,%lo12(.LBB0_3)
.LBB0_2:                                # %if.end
	LW X1,4072(X8)
	ADDI X10,X1,1
	LUI X1,%hi20(recursion)
	JALR X1,%lo12(recursion)(X1)
	SW X10,4076(X8)
	JAL X0,%lo12(.LBB0_3)
.LBB0_3:                                # %return
	LW X10,4076(X8)
	LD X8,16(X2)
	LD X2,24(X2)
	ADDI X2,X2,32
	JALR X0,0(X1)
.Lfunc_end0:
	.size	recursion, .Lfunc_end0-recursion
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym recursion