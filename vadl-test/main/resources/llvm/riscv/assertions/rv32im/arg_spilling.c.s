	.text
	.file	"arg_spilling.c"
	.globl	arg_spilling                    # -- Begin function arg_spilling
	.type	arg_spilling,@function
arg_spilling:                           # @arg_spilling
# %bb.0:                                # %entry
	ADDI t0,zero,0
	ADDI t0,t0,-48
	ADD sp,sp,t0
	SW fp,44(sp)                            # 4-byte Folded Spill
	SW ra,40(sp)                            # 4-byte Folded Spill
	ADDI t0,zero,0
	ADDI t0,t0,48
	ADD fp,sp,t0
	LW t0,0(fp)
	SW a0,-12(fp)
	SW a1,-16(fp)
	SW a2,-20(fp)
	SW a3,-24(fp)
	SW a4,-28(fp)
	SW a5,-32(fp)
	SW a6,-36(fp)
	SW a7,-40(fp)
	LW a0,-12(fp)
	LW a1,-16(fp)
	ADD a0,a0,a1
	LW a1,-20(fp)
	ADD a0,a0,a1
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
	LW a1,0(fp)
	ADD a0,a0,a1
	LW ra,40(sp)                            # 4-byte Folded Spill
	LW fp,44(sp)                            # 4-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,48
	ADD sp,sp,a1
	JALR zero,0(ra)
.Lfunc_end0:
	.size	arg_spilling, .Lfunc_end0-arg_spilling
                                        # -- End function
	.globl	arg_spilling_call               # -- Begin function arg_spilling_call
	.type	arg_spilling_call,@function
arg_spilling_call:                      # @arg_spilling_call
# %bb.0:                                # %entry
	ADDI a0,zero,0
	ADDI a0,a0,-16
	ADD sp,sp,a0
	SW fp,12(sp)                            # 4-byte Folded Spill
	SW ra,8(sp)                             # 4-byte Folded Spill
	ADDI a0,zero,0
	ADDI a0,a0,16
	ADD fp,sp,a0
	ADDI a0,zero,9
	SW a0,0(sp)
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
	LW ra,8(sp)                             # 4-byte Folded Spill
	LW fp,12(sp)                            # 4-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,16
	ADD sp,sp,a1
	JALR zero,0(ra)
.Lfunc_end1:
	.size	arg_spilling_call, .Lfunc_end1-arg_spilling_call
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym arg_spilling
