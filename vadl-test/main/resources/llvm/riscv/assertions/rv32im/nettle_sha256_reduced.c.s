	.text
	.file	"nettle_sha256_reduced.c"
	.globl	_nettle_sha256_compress         # -- Begin function _nettle_sha256_compress
	.type	_nettle_sha256_compress,@function
_nettle_sha256_compress:                # @_nettle_sha256_compress
# %bb.0:                                # %entry
	ADDI a3,zero,0
	ADDI a3,a3,-144
	ADD sp,sp,a3
	SW fp,140(sp)                           # 4-byte Folded Spill
	SW ra,136(sp)                           # 4-byte Folded Spill
	ADDI a3,zero,0
	ADDI a3,a3,144
	ADD fp,sp,a3
	SW a0,-12(fp)
	SW a1,-16(fp)
	SW a2,-20(fp)
	ADDI a0,zero,0
	SW a0,-120(fp)
	JAL zero,.LBB0_1
.LBB0_1:                                # %for.cond
                                        # =>This Inner Loop Header: Depth=1
	LW a1,-120(fp)
	ADDI a0,zero,15
	BLTU a0,a1,.LBB0_4
	JAL zero,.LBB0_2
.LBB0_2:                                # %for.body
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a1,-16(fp)
	LW a0,0(a1)
	ADDI a2,zero,24
	SLL a0,a0,a2
	LW a2,4(a1)
	ADDI a3,zero,16
	SLL a2,a2,a3
	OR a0,a0,a2
	LW a2,8(a1)
	SLLI a2,a2,8
	OR a0,a0,a2
	LW a1,12(a1)
	OR a1,a0,a1
	LW a0,-120(fp)
	SLLI a2,a0,2
	ADDI a0,fp,-84
	ADD a0,a0,a2
	SW a1,0(a0)
	JAL zero,.LBB0_3
.LBB0_3:                                # %for.inc
                                        #   in Loop: Header=BB0_1 Depth=1
	LW a0,-120(fp)
	ADDI a0,a0,1
	SW a0,-120(fp)
	LW a0,-16(fp)
	ADDI a0,a0,16
	SW a0,-16(fp)
	JAL zero,.LBB0_1
.LBB0_4:                                # %for.end
	ADDI a0,zero,0
	SW a0,-128(fp)
	ADDI a0,zero,1
	SW a0,-132(fp)
	LW a0,-132(fp)
	SLLI a1,a0,2
	ADDI a0,fp,-84
	ADD a1,a0,a1
	LW a2,0(a1)
	ADDI a1,zero,25
	SLL a1,a2,a1
	SRAI a2,a2,7
	OR a2,a1,a2
	LW a1,-128(fp)
	SLLI a1,a1,2
	ADD a0,a0,a1
	LW a1,0(a0)
	ADD a1,a1,a2
	SW a1,0(a0)
	LW ra,136(sp)                           # 4-byte Folded Spill
	LW fp,140(sp)                           # 4-byte Folded Spill
	ADDI a0,zero,0
	ADDI a0,a0,144
	ADD sp,sp,a0
	JALR zero,0(ra)
.Lfunc_end0:
	.size	_nettle_sha256_compress, .Lfunc_end0-_nettle_sha256_compress
                                        # -- End function
	.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
