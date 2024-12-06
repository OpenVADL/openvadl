	.text
	.file	"nettle_sha256_reduced.c"
	.globl	_nettle_sha256_compress         # -- Begin function _nettle_sha256_compress
	.type	_nettle_sha256_compress,@function
_nettle_sha256_compress:                # @_nettle_sha256_compress
# %bb.0:                                # %entry
	ADDI sp,sp,-160
	SD fp,152(sp)
	ADDI fp,sp,160
	SD a0,-16(fp)
	SD a1,-24(fp)
	SD a2,-32(fp)
	ADDI ra,zero,0
	SW ra,-132(fp)
	JAL zero,%hi(.LBB0_1)
.LBB0_1:                                # %for.cond
                                        # =>This Inner Loop Header: Depth=1
	LWU tp,-132(fp)
	ADDI ra,zero,15
	BLTU ra,tp,.LBB0_4
	JAL zero,%hi(.LBB0_2)
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	LD tp,-24(fp)
	LW ra,0(tp)
	SLLI ra,ra,24
	ADDI t0,tp,4
	LW t0,0(t0)
	SLLI t0,t0,16
	OR ra,ra,t0
	ADDI t0,tp,8
	LW t0,0(t0)
	SLLI t0,t0,8
	OR ra,ra,t0
	ADDI tp,tp,12
	LW tp,0(tp)
	OR tp,ra,tp
	LW ra,-132(fp)
	SLLI t0,ra,2
	ADDI ra,fp,-96
	ADD ra,ra,t0
	SW tp,0(ra)
	JAL zero,%hi(.LBB0_3)
.LBB0_3:                                # %for.inc
                                        #   in Loop: Header=BB0_1 Depth=1
	LW ra,-132(fp)
	ADDI ra,ra,1
	SW ra,-132(fp)
	LD ra,-24(fp)
	ADDI ra,ra,16
	SD ra,-24(fp)
	JAL zero,%hi(.LBB0_1)
.LBB0_4:                                # %for.end
	ADDI ra,zero,0
	SW ra,-144(fp)
	ADDI ra,zero,1
	SW ra,-148(fp)
	LW ra,-148(fp)
	SLLI tp,ra,2
	ADDI ra,fp,-96
	ADD tp,ra,tp
	LW t0,0(tp)
	SLLI tp,t0,25
	SRLI t0,t0,7
	OR t0,tp,t0
	LW tp,-144(fp)
	SLLI tp,tp,2
	ADD ra,ra,tp
	LW tp,0(ra)
	ADD tp,tp,t0
	SW tp,0(ra)
	LD fp,152(sp)
	ADDI sp,sp,160
	JALR zero,0(ra)
.Lfunc_end0:
	.size	_nettle_sha256_compress, .Lfunc_end0-_nettle_sha256_compress
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
