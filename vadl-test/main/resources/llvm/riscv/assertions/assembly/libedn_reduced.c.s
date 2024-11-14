.text
.file	"libedn_reduced.c"
.globl	jpegdct                         # -- Begin function jpegdct
.type	jpegdct,@function
jpegdct:                                # @jpegdct
# %bb.0:                                # %entry
ADDI sp,sp,-112
SD fp,104(sp)
ADDI fp,sp,112
SD a0,-16(fp)
SD a1,-24(fp)
ADDI ra,zero,1
SH ra,-78(fp)
ADDI ra,zero,0
SH ra,-80(fp)
ADDI ra,zero,13
SH ra,-82(fp)
ADDI ra,zero,8
SH ra,-84(fp)
JAL zero,%lo12(.LBB0_1)
.LBB0_1:                                # %for.cond
                                      # =>This Loop Header: Depth=1
                                      #     Child Loop BB0_3 Depth 2
                                      #       Child Loop BB0_5 Depth 3
LH tp,-78(fp)
ADDI ra,zero,8
BLT ra,tp,.LBB0_12
JAL zero,%lo12(.LBB0_2)
.LBB0_2:                                # %for.body
                                      #   in Loop: Header=BB0_1 Depth=1
ADDI ra,zero,0
SH ra,-74(fp)
JAL zero,%lo12(.LBB0_3)
.LBB0_3:                                # %for.cond2
                                      #   Parent Loop BB0_1 Depth=1
                                      # =>  This Loop Header: Depth=2
                                      #       Child Loop BB0_5 Depth 3
LH tp,-74(fp)
ADDI ra,zero,7
BLT ra,tp,.LBB0_10
JAL zero,%lo12(.LBB0_4)
.LBB0_4:                                # %for.body6
                                      #   in Loop: Header=BB0_3 Depth=2
ADDI ra,zero,0
SH ra,-76(fp)
JAL zero,%lo12(.LBB0_5)
.LBB0_5:                                # %for.cond7
                                      #   Parent Loop BB0_1 Depth=1
                                      #     Parent Loop BB0_3 Depth=2
                                      # =>    This Inner Loop Header: Depth=3
LH tp,-76(fp)
ADDI ra,zero,3
BLT ra,tp,.LBB0_8
JAL zero,%lo12(.LBB0_6)
.LBB0_6:                                # %for.body11
                                      #   in Loop: Header=BB0_5 Depth=3
LD X6,-16(fp)
LH X7,-78(fp)
LH ra,-76(fp)
MUL tp,X7,ra
SLLI tp,tp,1
ADD tp,X6,tp
LH tp,0(tp)
ADDI X5,zero,7
SUB a0,X5,ra
MUL X7,X7,a0
ADDI a0,zero,32
SLL X7,X7,a0
SRA X7,X7,a0
SLLI X7,X7,1
ADD X6,X6,X7
LH X6,0(X6)
ADD X6,tp,X6
SLLI tp,ra,2
ADDI ra,fp,-72
ADD tp,ra,tp
SW X6,0(tp)
LD X6,-16(fp)
LH X7,-78(fp)
LH a1,-76(fp)
MUL tp,X7,a1
SLLI tp,tp,1
ADD tp,X6,tp
LH tp,0(tp)
SUB X5,X5,a1
MUL X7,X7,X5
SLL X7,X7,a0
SRA X7,X7,a0
SLLI X7,X7,1
ADD X6,X6,X7
LH X6,0(X6)
SUB tp,tp,X6
SLLI X5,X5,2
ADD ra,ra,X5
SW tp,0(ra)
JAL zero,%lo12(.LBB0_7)
.LBB0_7:                                # %for.inc
                                      #   in Loop: Header=BB0_5 Depth=3
LH ra,-76(fp)
ADDI ra,ra,1
SH ra,-76(fp)
JAL zero,%lo12(.LBB0_5)
.LBB0_8:                                # %for.end
                                      #   in Loop: Header=BB0_3 Depth=2
LW ra,-72(fp)
LW tp,-60(fp)
ADD ra,ra,tp
SW ra,-40(fp)
LW ra,-72(fp)
LW tp,-60(fp)
SUB ra,ra,tp
SW ra,-36(fp)
LW ra,-68(fp)
LW tp,-64(fp)
ADD ra,ra,tp
SW ra,-32(fp)
LW ra,-68(fp)
LW tp,-64(fp)
SUB ra,ra,tp
SW ra,-28(fp)
LW ra,-40(fp)
LW tp,-32(fp)
ADD ra,ra,tp
ADDI X6,zero,32
SD X6,-100(fp)
SLL ra,ra,X6
SRA ra,ra,X6
LH tp,-80(fp)
ANDI tp,tp,4294967295
SRA tp,ra,tp
LD ra,-16(fp)
SH tp,0(ra)
LW ra,-40(fp)
LW tp,-32(fp)
SUB ra,ra,tp
SLL ra,ra,X6
SRA ra,ra,X6
LH tp,-80(fp)
ANDI tp,tp,4294967295
SRA tp,ra,tp
LD ra,-16(fp)
LH X5,-78(fp)
SLLI X5,X5,3
ADD ra,ra,X5
SH tp,0(ra)
LW ra,-28(fp)
LW tp,-36(fp)
ADD ra,ra,tp
ADDI tp,zero,48
SD tp,-92(fp)
SLL ra,ra,tp
SRA ra,ra,tp
LD X5,-24(fp)
LH X5,20(X5)
MUL ra,ra,X5
SW ra,-40(fp)
LW ra,-40(fp)
LW X5,-36(fp)
LD X7,-24(fp)
LH X7,18(X7)
MUL X5,X5,X7
SLL X5,X5,X6
SRA X5,X5,X6
LH X7,-82(fp)
ANDI X7,X7,4294967295
SRA X5,X5,X7
ADD X5,ra,X5
LD ra,-16(fp)
LH X7,-78(fp)
SLLI X7,X7,2
ADD ra,ra,X7
SH X5,0(ra)
LW ra,-40(fp)
LW X5,-28(fp)
LD X7,-24(fp)
LH X7,22(X7)
MUL X5,X5,X7
SLL X5,X5,X6
SRA X5,X5,X6
LH X6,-82(fp)
ANDI X6,X6,4294967295
SRA X5,X5,X6
ADD X5,ra,X5
LD ra,-16(fp)
LH X6,-78(fp)
ADDI X7,zero,12
MUL X6,X6,X7
ADD ra,ra,X6
SH X5,0(ra)
LW ra,-56(fp)
LW X5,-44(fp)
ADD ra,ra,X5
SLL ra,ra,tp
SRA ra,ra,tp
LD X5,-24(fp)
LH X5,4(X5)
MUL ra,ra,X5
SW ra,-72(fp)
LW ra,-52(fp)
LW X5,-48(fp)
ADD ra,ra,X5
SLL ra,ra,tp
SRA ra,ra,tp
LD X5,-24(fp)
LH X5,0(X5)
MUL ra,ra,X5
SW ra,-68(fp)
LW ra,-56(fp)
LW X5,-48(fp)
ADD ra,ra,X5
SW ra,-64(fp)
LW ra,-52(fp)
LW X5,-44(fp)
ADD ra,ra,X5
SW ra,-60(fp)
LW ra,-64(fp)
LW X5,-60(fp)
ADD ra,ra,X5
SLL ra,ra,tp
SRA ra,ra,tp
LD X5,-24(fp)
LH X5,16(X5)
MUL ra,ra,X5
SW ra,-40(fp)
LH ra,-64(fp)
LD X5,-24(fp)
LH X5,2(X5)
MUL ra,ra,X5
LW X5,-40(fp)
ADD ra,ra,X5
SW ra,-64(fp)
LH ra,-60(fp)
LD X5,-24(fp)
LH X5,6(X5)
MUL ra,ra,X5
LW X5,-40(fp)
ADD ra,ra,X5
SW ra,-60(fp)
LW ra,-56(fp)
LD X5,-24(fp)
ADDI X5,X5,8
LH X5,0(X5)
MUL ra,ra,X5
LW X5,-72(fp)
ADD ra,ra,X5
LW X5,-64(fp)
ADD ra,ra,X5
SLL ra,ra,tp
SRA ra,ra,tp
LH X5,-82(fp)
ANDI X5,X5,4294967295
SRA X5,ra,X5
LD ra,-16(fp)
LH X6,-78(fp)
ADDI X7,zero,14
MUL X6,X6,X7
ADD ra,ra,X6
SH X5,0(ra)
LW ra,-52(fp)
LD X5,-24(fp)
ADDI X5,X5,12
LH X5,0(X5)
MUL ra,ra,X5
LW X5,-68(fp)
ADD ra,ra,X5
LW X5,-60(fp)
ADD ra,ra,X5
SLL ra,ra,tp
SRA ra,ra,tp
LH X5,-82(fp)
ANDI X5,X5,4294967295
SRA X5,ra,X5
LD ra,-16(fp)
LH X6,-78(fp)
ADDI X7,zero,10
MUL X6,X6,X7
ADD ra,ra,X6
SH X5,0(ra)
LW ra,-48(fp)
LD X5,-24(fp)
ADDI X5,X5,10
LH X5,0(X5)
MUL ra,ra,X5
LW X5,-68(fp)
ADD ra,ra,X5
LW X5,-64(fp)
ADD ra,ra,X5
SLL ra,ra,tp
SRA ra,ra,tp
LH X5,-82(fp)
ANDI X5,X5,4294967295
SRA X5,ra,X5
LD ra,-16(fp)
LH X6,-78(fp)
ADDI X7,zero,6
MUL X6,X6,X7
ADD ra,ra,X6
SH X5,0(ra)
LW ra,-44(fp)
LD X5,-24(fp)
ADDI X5,X5,14
LH X5,0(X5)
MUL ra,ra,X5
LW X5,-72(fp)
ADD ra,ra,X5
LW X5,-60(fp)
ADD ra,ra,X5
SLL ra,ra,tp
SRA ra,ra,tp
LH tp,-82(fp)
ANDI tp,tp,4294967295
SRA tp,ra,tp
LD ra,-16(fp)
LH X5,-78(fp)
SLLI X5,X5,1
ADD ra,ra,X5
SH tp,0(ra)
JAL zero,%lo12(.LBB0_9)
.LBB0_9:                                # %for.inc221
                                      #   in Loop: Header=BB0_3 Depth=2
LH ra,-74(fp)
ADDI ra,ra,1
SH ra,-74(fp)
LH tp,-84(fp)
LD ra,-16(fp)
SLLI tp,tp,1
ADD ra,ra,tp
SD ra,-16(fp)
JAL zero,%lo12(.LBB0_3)
.LBB0_10:                               # %for.end224
                                      #   in Loop: Header=BB0_1 Depth=1
JAL zero,%lo12(.LBB0_11)
.LBB0_11:                               # %for.inc225
                                      #   in Loop: Header=BB0_1 Depth=1
LH ra,-78(fp)
ADDI ra,ra,7
SH ra,-78(fp)
LH ra,-80(fp)
ADDI ra,ra,3
SH ra,-80(fp)
LH ra,-82(fp)
ADDI ra,ra,3
SH ra,-82(fp)
LH ra,-84(fp)
ADDI ra,ra,-7
SH ra,-84(fp)
LD ra,-16(fp)
ADDI ra,ra,-128
SD ra,-16(fp)
JAL zero,%lo12(.LBB0_1)
.LBB0_12:                               # %for.end239
LD fp,104(sp)
ADDI sp,sp,112
JALR zero,0(ra)
.Lfunc_end0:
.size	jpegdct, .Lfunc_end0-jpegdct
                                      # -- End function
.ident	"clang version 17.0.6 (https://github.com/llvm/llvm-project.git 6009708b4367171ccdbf4b5905cb6a803753fe18)"
.section	".note.GNU-stack","",@progbits
.addrsig