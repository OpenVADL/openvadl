	.text
	.file	"arg_spilling.c"
	.globl	arg_spilling                    # -- Begin function arg_spilling
	.type	arg_spilling,@function
arg_spilling:                           # @arg_spilling
# %bb.0:                                # %entry
	ADDI sp,sp,-64
	SD fp,56(sp)
	SD ra,48(sp)
	ADDI fp,sp,64
	ADDI t0,a0,0
	LD a0,0(fp)
                                        # kill: def $x6 killed $x17
                                        # kill: def $x6 killed $x16
                                        # kill: def $x6 killed $x15
                                        # kill: def $x6 killed $x14
                                        # kill: def $x6 killed $x13
                                        # kill: def $x6 killed $x12
                                        # kill: def $x6 killed $x11
                                        # kill: def $x6 killed $x5
	SW t0,-20(fp)
	SW a1,-24(fp)
	SW a2,-28(fp)
	SW a3,-32(fp)
	SW a4,-36(fp)
	SW a5,-40(fp)
	SW a6,-44(fp)
	SW a7,-48(fp)
	SW a0,-52(fp)
	LW a0,-20(fp)
	LW a1,-24(fp)
	ADD a0,a0,a1
	LW a1,-28(fp)
	ADD a0,a0,a1
	LW a1,-32(fp)
	ADD a0,a0,a1
	LW a1,-36(fp)
	ADD a0,a0,a1
	LW a1,-40(fp)
	ADD a0,a0,a1
	LW a1,-44(fp)
	ADD a0,a0,a1
	LW a1,-48(fp)
	ADD a0,a0,a1
	LW a1,-52(fp)
	ADD a0,a0,a1
	LD ra,48(sp)
	LD fp,56(sp)
	ADDI sp,sp,64
	JALR zero,0(ra)
.Lfunc_end0:
	.size	arg_spilling, .Lfunc_end0-arg_spilling
                                        # -- End function
	.globl	arg_spilling_call               # -- Begin function arg_spilling_call
	.type	arg_spilling_call,@function
arg_spilling_call:                      # @arg_spilling_call
# %bb.0:                                # %entry
	ADDI sp,sp,-32
	SD fp,24(sp)
	SD ra,16(sp)
	ADDI fp,sp,32
	ADDI a0,zero,9
	SD a0,0(sp)
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
	LD ra,16(sp)
	LD fp,24(sp)
	ADDI sp,sp,32
	JALR zero,0(ra)
.Lfunc_end1:
	.size	arg_spilling_call, .Lfunc_end1-arg_spilling_call
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym arg_spilling
