	.text
	.file	"arg_spilling.c"
	.globl	arg_spilling                    # -- Begin function arg_spilling
	.type	arg_spilling,@function
arg_spilling:                           # @arg_spilling
# %bb.0:                                # %entry
	ADDI sp,sp,-48
	SD fp,40(sp)
	ADDI fp,sp,48
	LD ra,0(fp)
                                        # kill: def $x4 killed $x17
                                        # kill: def $x4 killed $x16
                                        # kill: def $x4 killed $x15
                                        # kill: def $x4 killed $x14
                                        # kill: def $x4 killed $x13
                                        # kill: def $x4 killed $x12
                                        # kill: def $x4 killed $x11
                                        # kill: def $x4 killed $x10
	SW a0,-12(fp)
	SW a1,-16(fp)
	SW a2,-20(fp)
	SW a3,-24(fp)
	SW a4,-28(fp)
	SW a5,-32(fp)
	SW a6,-36(fp)
	SW a7,-40(fp)
	SW ra,-44(fp)
	LW ra,-12(fp)
	LW tp,-16(fp)
	ADD ra,ra,tp
	LW tp,-20(fp)
	ADD ra,ra,tp
	LW tp,-24(fp)
	ADD ra,ra,tp
	LW tp,-28(fp)
	ADD ra,ra,tp
	LW tp,-32(fp)
	ADD ra,ra,tp
	LW tp,-36(fp)
	ADD ra,ra,tp
	LW tp,-40(fp)
	ADD ra,ra,tp
	LW tp,-44(fp)
	ADD a0,ra,tp
	LD fp,40(sp)
	ADDI sp,sp,48
	JALR zero,0(ra)
.Lfunc_end0:
	.size	arg_spilling, .Lfunc_end0-arg_spilling
                                        # -- End function
	.globl	arg_spilling_call               # -- Begin function arg_spilling_call
	.type	arg_spilling_call,@function
arg_spilling_call:                      # @arg_spilling_call
# %bb.0:                                # %entry
	ADDI sp,sp,-32
	SD sp,24(sp)
	SD fp,16(sp)
	ADDI fp,sp,32
	ADDI ra,zero,9
	SD ra,0(sp)
	ADDI a0,zero,1
	ADDI a1,zero,2
	ADDI a2,zero,3
	ADDI a3,zero,4
	ADDI a4,zero,5
	ADDI a5,zero,6
	ADDI a6,zero,7
	ADDI a7,zero,8
	LUI ra,%hi(arg_spilling)
	JALR ra,%lo(arg_spilling)(ra)
	LD fp,16(sp)
	LD sp,24(sp)
	ADDI sp,sp,32
	JALR zero,0(ra)
.Lfunc_end1:
	.size	arg_spilling_call, .Lfunc_end1-arg_spilling_call
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym arg_spilling
