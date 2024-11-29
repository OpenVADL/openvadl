.text
.file	"nettle_sha256_reduced.c"
.globl	_nettle_sha256_compress         # -- Begin function _nettle_sha256_compress
.type	_nettle_sha256_compress,@function
_nettle_sha256_compress:                # @_nettle_sha256_compress
# %bb.0:                                # %entry
ADDI sp,sp,-128
SW fp,124(sp)
ADDI fp,sp,128
SW a0,-8(fp)
SW a1,-12(fp)
SW a2,-16(fp)
ADDI ra,zero,0
SW ra,-116(fp)
JAL zero,%lo12(.LBB0_1)
.LBB0_1:                                # %for.cond
                                      # =>This Inner Loop Header: Depth=1
LW tp,-116(fp)
ADDI ra,zero,15
BLTU ra,tp,.LBB0_4
JAL zero,%lo12(.LBB0_2)
.LBB0_2:                                # %for.body
                                      #   in Loop: Header=BB0_1 Depth=1
LW tp,-12(fp)
LW ra,0(tp)
ADDI X5,zero,24
SLL ra,ra,X5
LW X5,4(tp)
ADDI X6,zero,16
SLL X5,X5,X6
OR ra,ra,X5
LW X5,8(tp)
SLLI X5,X5,8
OR ra,ra,X5
LW tp,12(tp)
OR tp,ra,tp
LW ra,-116(fp)
SLLI X5,ra,2
ADDI ra,fp,-80
ADD ra,ra,X5
SW tp,0(ra)
JAL zero,%lo12(.LBB0_3)
.LBB0_3:                                # %for.inc
                                      #   in Loop: Header=BB0_1 Depth=1
LW ra,-116(fp)
ADDI ra,ra,1
SW ra,-116(fp)
LW ra,-12(fp)
ADDI ra,ra,16
SW ra,-12(fp)
JAL zero,%lo12(.LBB0_1)
.LBB0_4:                                # %for.end
ADDI ra,zero,0
SW ra,-124(fp)
ADDI ra,zero,1
SW ra,-128(fp)
LW ra,-128(fp)
SLLI tp,ra,2
ADDI ra,fp,-80
ADD tp,ra,tp
LW X5,0(tp)
ADDI tp,zero,25
SLL tp,X5,tp
SRAI X5,X5,7
OR X5,tp,X5
LW tp,-124(fp)
SLLI tp,tp,2
ADD ra,ra,tp
LW tp,0(ra)
ADD tp,tp,X5
SW tp,0(ra)
LW fp,124(sp)
ADDI sp,sp,128
JALR zero,0(ra)
.Lfunc_end0:
.size	_nettle_sha256_compress, .Lfunc_end0-_nettle_sha256_compress
                                      # -- End function
.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig
