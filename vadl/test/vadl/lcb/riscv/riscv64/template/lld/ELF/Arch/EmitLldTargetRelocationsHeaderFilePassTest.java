// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.riscv.riscv64.template.lld.ELF.Arch;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.lld.ELF.Arch.EmitLldTargetRelocationsHeaderFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitLldTargetRelocationsHeaderFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(
            EmitLldTargetRelocationsHeaderFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(
                EmitLldTargetRelocationsHeaderFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #include "vadl-builtins.h"
        
        
        int64_t RV3264Base_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264Base_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264Base_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264Base_pcrel_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264Base_got_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264Base_Itype_ABSOLUTE_ADDI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_ADDI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_ANDI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_ANDI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_ORI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_ORI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_XORI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_XORI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_SLTI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_SLTI_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_SLTIU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_SLTIU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Utype_ABSOLUTE_AUIPC_immUp(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Utype_RELATIVE_AUIPC_immUp(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Utype_ABSOLUTE_LUI_immUp(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Utype_RELATIVE_LUI_immUp(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_LB_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_LB_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_LBU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_LBU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_LH_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_LH_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_LHU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_LHU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_LW_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_LW_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Stype_ABSOLUTE_SB_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Stype_RELATIVE_SB_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Stype_ABSOLUTE_SH_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Stype_RELATIVE_SH_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Stype_ABSOLUTE_SW_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Stype_RELATIVE_SW_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_ABSOLUTE_BEQ_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_RELATIVE_BEQ_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_ABSOLUTE_BNE_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_RELATIVE_BNE_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_ABSOLUTE_BGE_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_RELATIVE_BGE_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_ABSOLUTE_BGEU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_RELATIVE_BGEU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_ABSOLUTE_BLT_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_RELATIVE_BLT_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_ABSOLUTE_BLTU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Btype_RELATIVE_BLTU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Jtype_ABSOLUTE_JAL_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Jtype_RELATIVE_JAL_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_JALR_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_JALR_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_LWU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_LWU_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_LD_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_LD_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Stype_ABSOLUTE_SD_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Stype_RELATIVE_SD_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_ABSOLUTE_ADDIW_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Itype_RELATIVE_ADDIW_immS(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Rtype_ABSOLUTE_SLLIW_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Rtype_RELATIVE_SLLIW_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Rtype_ABSOLUTE_SRLIW_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Rtype_RELATIVE_SRLIW_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Rtype_ABSOLUTE_SRAIW_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Rtype_RELATIVE_SRAIW_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Ftype_ABSOLUTE_SLLI_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Ftype_RELATIVE_SLLI_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Ftype_ABSOLUTE_SRLI_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Ftype_RELATIVE_SRLI_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Ftype_ABSOLUTE_SRAI_shamt(int64_t input) {
           return input;
        }
        int64_t RV3264Base_Ftype_RELATIVE_SRAI_shamt(int64_t input) {
           return input;
        }
        """.trim().lines(), output);
  }
}
