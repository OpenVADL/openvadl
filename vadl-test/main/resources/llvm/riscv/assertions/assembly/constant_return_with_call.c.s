.text
.file	"constant_return_with_call.c"
.globl	foo                             # -- Begin function foo
.type	foo,@function
foo:                                    # @foo
# %bb.0:                                # %entry
ADDI X2,X2,4080
SD X8,8(X2)
ADDI X8,X2,16
ADDI X10,X0,0
LD X8,8(X2)
ADDI X2,X2,16
JALR X0,0(X1)
.Lfunc_end0:
.size	foo, .Lfunc_end0-foo
                                      # -- End function
.globl	constant_return                 # -- Begin function constant_return
.type	constant_return,@function
constant_return:                        # @constant_return
# %bb.0:                                # %entry
ADDI X2,X2,4080
SD X2,8(X2)
SD X8,0(X2)
ADDI X8,X2,16
LUI X1,%hi20(foo)
JALR X1,%lo12(foo)(X1)
LD X8,0(X2)
LD X2,8(X2)
ADDI X2,X2,16
    JALR X0,0(X1)
  .Lfunc_end1:
    .size	constant_return, .Lfunc_end1-constant_return
                                          # -- End function
    .ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
    .section	".note.GNU-stack","",@progbits
    .addrsig
    .addrsig_sym foo