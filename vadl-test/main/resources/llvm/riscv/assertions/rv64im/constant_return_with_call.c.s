	.text
	.file	"constant_return_with_call.c"
	.globl	foo                             # -- Begin function foo
	.type	foo,@function
foo:                                    # @foo
# %bb.0:                                # %entry
	ADDI a0,zero,0
	ADDI a0,a0,-16
	ADD sp,sp,a0
	SD fp,8(sp)                             # 8-byte Folded Spill
	SD ra,0(sp)                             # 8-byte Folded Spill
	ADDI a0,zero,0
	ADDI a0,a0,16
	ADD fp,sp,a0
	ADDI a0,zero,0
	LD ra,0(sp)                             # 8-byte Folded Spill
	LD fp,8(sp)                             # 8-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,16
	ADD sp,sp,a1
	JALR zero,0(ra)
.Lfunc_end0:
	.size	foo, .Lfunc_end0-foo
                                        # -- End function
	.globl	constant_return                 # -- Begin function constant_return
	.type	constant_return,@function
constant_return:                        # @constant_return
# %bb.0:                                # %entry
	ADDI a0,zero,0
	ADDI a0,a0,-16
	ADD sp,sp,a0
	SD fp,8(sp)                             # 8-byte Folded Spill
	SD ra,0(sp)                             # 8-byte Folded Spill
	ADDI a0,zero,0
	ADDI a0,a0,16
	ADD fp,sp,a0
	LUI ra,%hi(foo)
	JALR ra,%lo(foo)(ra)
	LD ra,0(sp)                             # 8-byte Folded Spill
	LD fp,8(sp)                             # 8-byte Folded Spill
	ADDI a1,zero,0
	ADDI a1,a1,16
	ADD sp,sp,a1
	JALR zero,0(ra)
.Lfunc_end1:
	.size	constant_return, .Lfunc_end1-constant_return
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym foo
