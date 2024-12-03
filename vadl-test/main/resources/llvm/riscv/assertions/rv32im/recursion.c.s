.text
.file	"recursion.c"
.globl	recursion                       # -- Begin function recursion
.type	recursion,@function
recursion:                              # @recursion
# %bb.0:                                # %entry
ADDI sp,sp,-16
SW sp,12(sp)
SW fp,8(sp)
ADDI fp,sp,16
SW a0,-16(fp)
LW ra,-16(fp)
ADDI tp,zero,1
BLT ra,tp,.LBB0_2
JAL zero,%lo12(.LBB0_1)
.LBB0_1:                                # %if.then
LW ra,-16(fp)
SW ra,-12(fp)
JAL zero,%lo12(.LBB0_3)
.LBB0_2:                                # %if.end
LW ra,-16(fp)
ADDI a0,ra,1
LUI ra,%hi20(recursion)
JALR ra,%lo12(recursion)(ra)
SW a0,-12(fp)
JAL zero,%lo12(.LBB0_3)
.LBB0_3:                                # %return
LW a0,-12(fp)
LW fp,8(sp)
LW sp,12(sp)
ADDI sp,sp,16
JALR zero,0(ra)
.Lfunc_end0:
.size	recursion, .Lfunc_end0-recursion
                                      # -- End function
.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig
.addrsig_sym recursion
