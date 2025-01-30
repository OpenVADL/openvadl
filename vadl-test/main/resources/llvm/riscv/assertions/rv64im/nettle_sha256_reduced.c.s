	.text
	.file	"nettle_sha256_reduced.c"
	.globl	_nettle_sha256_compress         # -- Begin function _nettle_sha256_compress
	.type	_nettle_sha256_compress,@function
_nettle_sha256_compress:                # @_nettle_sha256_compress
# %bb.0:                                # %entry
	ADDI a3,zero,0
	ADDI a3,a3,-160
	ADD sp,sp,a3
	SD fp,152(sp)                           # 8-byte Folded Spill
	SD ra,144(sp)                           # 8-byte Folded Spill
	ADDI a3,zero,0
	ADDI a3,a3,160
	ADD fp,sp,a3
	SD a0,-24(fp)
	SD a1,-32(fp)
	SD a2,-40(fp)
	ADDI a0,zero,0
	SW a0,-140(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Inner Loop Header: Depth=1
	LWU a1,-140(fp)
	ADDI a0,zero,15
	BLTU a0,a1,.LBB0_4
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	LD a1,-32(fp)
	LW a0,0(a1)
	SLLI a0,a0,24
	ADDI a2,a1,4
	LW a2,0(a2)
	SLLI a2,a2,16
	OR a0,a0,a2
	ADDI a2,a1,8
	LW a2,0(a2)
	SLLI a2,a2,8
	OR a0,a0,a2
	ADDI a1,a1,12
	LW a1,0(a1)
	OR a1,a0,a1
	LW a0,-140(fp)
	SLLI a2,a0,2
	ADDI a0,fp,-104
	ADD a0,a0,a2
	SW a1,0(a0)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.inc
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-140(fp)
	ADDI a0,a0,1
	SW a0,-140(fp)
	LD a0,-32(fp)
	ADDI a0,a0,16
	SD a0,-32(fp)
	JAL zero,.LBB0_1
.LBB0_4:                                # %for.end
	ADDI a0,zero,0
	SW a0,-152(fp)
	ADDI a0,zero,1
	SW a0,-156(fp)
	LW a0,-156(fp)
	SLLI a1,a0,2
	ADDI a0,fp,-104
	ADD a1,a0,a1
	LW a2,0(a1)
	SLLI a1,a2,25
	SRLI a2,a2,7
	OR a2,a1,a2
	LW a1,-152(fp)
	SLLI a1,a1,2
	ADD a0,a0,a1
	LW a1,0(a0)
	ADD a1,a1,a2
	SW a1,0(a0)
	LD ra,144(sp)                           # 8-byte Folded Spill
	LD fp,152(sp)                           # 8-byte Folded Spill
	ADDI a0,zero,0
	ADDI a0,a0,160
	ADD sp,sp,a0
	JALR zero,0(ra)
.Lfunc_end0:
	.size	_nettle_sha256_compress, .Lfunc_end0-_nettle_sha256_compress
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
