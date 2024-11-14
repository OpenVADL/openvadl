.text
.file	"and.c"
.globl	and                             # -- Begin function and
.type	and,@function
and:                                    # @and
# %bb.0:                                # %entry
ADDI sp,sp,-16
SD fp,8(sp)
ADDI fp,sp,16
                                      # kill: def $x1 killed $x11
                                      # kill: def $x1 killed $x10
SW a0,-12(fp)
SW a1,-16(fp)
LW ra,-12(fp)
LW tp,-16(fp)
AND a0,ra,tp
LD fp,8(sp)
ADDI sp,sp,16
JALR zero,0(ra)
.Lfunc_end0:
.size	and, .Lfunc_end0-and
                                      # -- End function
.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig