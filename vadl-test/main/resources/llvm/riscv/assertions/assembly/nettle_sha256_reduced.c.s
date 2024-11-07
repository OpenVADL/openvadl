.text
.file	"nettle_sha256_reduced.c"
.globl	_nettle_sha256_compress         # -- Begin function _nettle_sha256_compress
.type	_nettle_sha256_compress,@function
_nettle_sha256_compress:                # @_nettle_sha256_compress
# %bb.0:                                # %entry
ADDI X2,X2,3936
SD X8,152(X2)
ADDI X8,X2,160
SD X10,4080(X8)
SD X11,4072(X8)
SD X12,4064(X8)
ADDI X1,X0,0
SW X1,3964(X8)
JAL X0,%lo12(.LBB0_1)
.LBB0_1:                                # %for.cond
                                      # =>This Inner Loop Header: Depth=1
LWU X4,3964(X8)
ADDI X1,X0,15
BLTU X1,X4,.LBB0_4
JAL X0,%lo12(.LBB0_2)
.LBB0_2:                                # %for.body
                                      #   in Loop: Header=BB0_1 Depth=1
LD X4,4072(X8)
LW X1,0(X4)
SLLI X1,X1,24
ADDI X5,X4,4
LW X5,0(X5)
SLLI X5,X5,16
OR X1,X1,X5
ADDI X5,X4,8
LW X5,0(X5)
SLLI X5,X5,8
OR X1,X1,X5
ADDI X4,X4,12
LW X4,0(X4)
OR X4,X1,X4
LW X1,3964(X8)
SLLI X5,X1,2
ADDI X1,X8,4000
ADD X1,X1,X5
SW X4,0(X1)
JAL X0,%lo12(.LBB0_3)
.LBB0_3:                                # %for.inc
                                      #   in Loop: Header=BB0_1 Depth=1
LW X1,3964(X8)
ADDI X1,X1,1
SW X1,3964(X8)
LD X1,4072(X8)
ADDI X1,X1,16
SD X1,4072(X8)
JAL X0,%lo12(.LBB0_1)
.LBB0_4:                                # %for.end
ADDI X1,X0,0
SW X1,3952(X8)
ADDI X1,X0,1
SW X1,3948(X8)
LW X1,3948(X8)
SLLI X4,X1,2
ADDI X1,X8,4000
ADD X4,X1,X4
LW X5,0(X4)
SLLI X4,X5,25
SRLI X5,X5,7
OR X5,X4,X5
LW X4,3952(X8)
SLLI X4,X4,2
ADD X1,X1,X4
LW X4,0(X1)
ADD X4,X4,X5
SW X4,0(X1)
LD X8,152(X2)
ADDI X2,X2,160
JALR X0,0(X1)
.Lfunc_end0:
.size	_nettle_sha256_compress, .Lfunc_end0-_nettle_sha256_compress
                                      # -- End function
.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig