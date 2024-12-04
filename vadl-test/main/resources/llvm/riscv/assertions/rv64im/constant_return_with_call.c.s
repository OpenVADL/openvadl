	.text
	.file	"constant_return_with_call.c"
	.globl	foo                             # -- Begin function foo
	.type	foo,@function
foo:                                    # @foo
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SD fp,8(sp)
	ADDI fp,sp,16
	ADDI a0,zero,0
	LD fp,8(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end0:
	.size	foo, .Lfunc_end0-foo
                                        # -- End function
	.globl	constant_return                 # -- Begin function constant_return
	.type	constant_return,@function
constant_return:                        # @constant_return
# %bb.0:                                # %entry
	ADDI sp,sp,-16
	SD sp,8(sp)
	SD fp,0(sp)
	ADDI fp,sp,16
	LUI ra,%hi(foo)
	JALR ra,%lo(foo)(ra)
	LD fp,0(sp)
	LD sp,8(sp)
	ADDI sp,sp,16
	JALR zero,0(ra)
.Lfunc_end1:
	.size	constant_return, .Lfunc_end1-constant_return
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym foo
