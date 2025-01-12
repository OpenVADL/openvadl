	.text
	.file	"constant_return.c"
	.globl	constant_return                 # -- Begin function constant_return
	.type	constant_return,@function
constant_return:                        # @constant_return
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SW fp,12(sp)
	ADDI fp,sp,16
	ADDI a0,zero,0
	LW fp,12(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end0:
	.size	constant_return, .Lfunc_end0-constant_return
                                        # -- End function
	.ident	"clang version 17.0.6 (https:/hub.com/llvm/llvm-project 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
