// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.riscv.riscv64.template.include.llvm.BinaryFormat.ELFRelocs;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitTargetElfRelocsDefFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitTargetElfRelocsDefFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitTargetElfRelocsDefFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #ifndef ELF_RELOC
        #error "ELF_RELOC must be defined"
        #endif
        
        ELF_RELOC(R_processornamevalue_NONE, 0)
        ELF_RELOC(R_processornamevalue_32, 1)
        ELF_RELOC(R_processornamevalue_64, 2)
        
        ELF_RELOC(R_RV3264Base_Btype_ABSOLUTE_BEQ_immS, 3)
        ELF_RELOC(R_RV3264Base_Btype_ABSOLUTE_BGEU_immS, 4)
        ELF_RELOC(R_RV3264Base_Btype_ABSOLUTE_BGE_immS, 5)
        ELF_RELOC(R_RV3264Base_Btype_ABSOLUTE_BLTU_immS, 6)
        ELF_RELOC(R_RV3264Base_Btype_ABSOLUTE_BLT_immS, 7)
        ELF_RELOC(R_RV3264Base_Btype_ABSOLUTE_BNE_immS, 8)
        ELF_RELOC(R_RV3264Base_Btype_RELATIVE_BEQ_immS, 9)
        ELF_RELOC(R_RV3264Base_Btype_RELATIVE_BGEU_immS, 10)
        ELF_RELOC(R_RV3264Base_Btype_RELATIVE_BGE_immS, 11)
        ELF_RELOC(R_RV3264Base_Btype_RELATIVE_BLTU_immS, 12)
        ELF_RELOC(R_RV3264Base_Btype_RELATIVE_BLT_immS, 13)
        ELF_RELOC(R_RV3264Base_Btype_RELATIVE_BNE_immS, 14)
        ELF_RELOC(R_RV3264Base_Ftype_ABSOLUTE_SLLI_shamt, 15)
        ELF_RELOC(R_RV3264Base_Ftype_ABSOLUTE_SRAI_shamt, 16)
        ELF_RELOC(R_RV3264Base_Ftype_ABSOLUTE_SRLI_shamt, 17)
        ELF_RELOC(R_RV3264Base_Ftype_RELATIVE_SLLI_shamt, 18)
        ELF_RELOC(R_RV3264Base_Ftype_RELATIVE_SRAI_shamt, 19)
        ELF_RELOC(R_RV3264Base_Ftype_RELATIVE_SRLI_shamt, 20)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_ADDIW_immS, 21)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_ADDI_immS, 22)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_ANDI_immS, 23)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_JALR_immS, 24)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_LBU_immS, 25)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_LB_immS, 26)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_LD_immS, 27)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_LHU_immS, 28)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_LH_immS, 29)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_LWU_immS, 30)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_LW_immS, 31)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_ORI_immS, 32)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_SLTIU_immS, 33)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_SLTI_immS, 34)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_XORI_immS, 35)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_ADDIW_immS, 36)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_ADDI_immS, 37)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_ANDI_immS, 38)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_JALR_immS, 39)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_LBU_immS, 40)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_LB_immS, 41)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_LD_immS, 42)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_LHU_immS, 43)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_LH_immS, 44)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_LWU_immS, 45)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_LW_immS, 46)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_ORI_immS, 47)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_SLTIU_immS, 48)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_SLTI_immS, 49)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_XORI_immS, 50)
        ELF_RELOC(R_RV3264Base_Jtype_ABSOLUTE_JAL_immS, 51)
        ELF_RELOC(R_RV3264Base_Jtype_RELATIVE_JAL_immS, 52)
        ELF_RELOC(R_RV3264Base_Rtype_ABSOLUTE_SLLIW_shamt, 53)
        ELF_RELOC(R_RV3264Base_Rtype_ABSOLUTE_SRAIW_shamt, 54)
        ELF_RELOC(R_RV3264Base_Rtype_ABSOLUTE_SRLIW_shamt, 55)
        ELF_RELOC(R_RV3264Base_Rtype_RELATIVE_SLLIW_shamt, 56)
        ELF_RELOC(R_RV3264Base_Rtype_RELATIVE_SRAIW_shamt, 57)
        ELF_RELOC(R_RV3264Base_Rtype_RELATIVE_SRLIW_shamt, 58)
        ELF_RELOC(R_RV3264Base_Stype_ABSOLUTE_SB_immS, 59)
        ELF_RELOC(R_RV3264Base_Stype_ABSOLUTE_SD_immS, 60)
        ELF_RELOC(R_RV3264Base_Stype_ABSOLUTE_SH_immS, 61)
        ELF_RELOC(R_RV3264Base_Stype_ABSOLUTE_SW_immS, 62)
        ELF_RELOC(R_RV3264Base_Stype_RELATIVE_SB_immS, 63)
        ELF_RELOC(R_RV3264Base_Stype_RELATIVE_SD_immS, 64)
        ELF_RELOC(R_RV3264Base_Stype_RELATIVE_SH_immS, 65)
        ELF_RELOC(R_RV3264Base_Stype_RELATIVE_SW_immS, 66)
        ELF_RELOC(R_RV3264Base_Utype_ABSOLUTE_AUIPC_immUp, 67)
        ELF_RELOC(R_RV3264Base_Utype_ABSOLUTE_LUI_immUp, 68)
        ELF_RELOC(R_RV3264Base_Utype_RELATIVE_AUIPC_immUp, 69)
        ELF_RELOC(R_RV3264Base_Utype_RELATIVE_LUI_immUp, 70)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Btype_imm, 71)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Ftype_sft, 72)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Itype_imm, 73)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Jtype_imm, 74)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Rtype_rs2, 75)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Stype_imm, 76)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Utype_imm, 77)
        ELF_RELOC(R_RV3264Base_hi_Btype_imm, 78)
        ELF_RELOC(R_RV3264Base_hi_Ftype_sft, 79)
        ELF_RELOC(R_RV3264Base_hi_Itype_imm, 80)
        ELF_RELOC(R_RV3264Base_hi_Jtype_imm, 81)
        ELF_RELOC(R_RV3264Base_hi_Rtype_rs2, 82)
        ELF_RELOC(R_RV3264Base_hi_Stype_imm, 83)
        ELF_RELOC(R_RV3264Base_hi_Utype_imm, 84)
        ELF_RELOC(R_RV3264Base_lo_Btype_imm, 85)
        ELF_RELOC(R_RV3264Base_lo_Ftype_sft, 86)
        ELF_RELOC(R_RV3264Base_lo_Itype_imm, 87)
        ELF_RELOC(R_RV3264Base_lo_Jtype_imm, 88)
        ELF_RELOC(R_RV3264Base_lo_Rtype_rs2, 89)
        ELF_RELOC(R_RV3264Base_lo_Stype_imm, 90)
        ELF_RELOC(R_RV3264Base_lo_Utype_imm, 91)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Btype_imm, 92)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Ftype_sft, 93)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Itype_imm, 94)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Jtype_imm, 95)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Rtype_rs2, 96)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Stype_imm, 97)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Utype_imm, 98)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Btype_imm, 99)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Ftype_sft, 100)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Itype_imm, 101)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Jtype_imm, 102)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Rtype_rs2, 103)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Stype_imm, 104)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Utype_imm, 105)
        """.trim().lines(), output);
  }
}
