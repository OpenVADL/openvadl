	.text
	.file	"large_constant.c"
	.globl	constant_return                 # -- Begin function constant_return
	.type	constant_return,@function
constant_return:                        # @constant_return
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SD fp,8(sp)
	ADDI fp,sp,16
                                        # kill: def $x1 killed $x10
	SW a0,-12(fp)
	LW ra,-12(fp)
	LUI tp,0x80
	ADDI tp,tp,-1
	ADD a0,ra,tp
	LD fp,8(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end0:
	.size	constant_return, .Lfunc_end0-constant_return
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
