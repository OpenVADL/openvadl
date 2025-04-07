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
        int64_t RV3264I_Itype_ABSOLUTE_imm_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Utype_ABSOLUTE_imm_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Stype_ABSOLUTE_imm_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Btype_ABSOLUTE_imm_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Jtype_ABSOLUTE_imm_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Rtype_ABSOLUTE_rs2_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Ftype_ABSOLUTE_sft_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Itype_ABSOLUTE_imm_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Utype_ABSOLUTE_imm_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Stype_ABSOLUTE_imm_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Btype_ABSOLUTE_imm_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Jtype_ABSOLUTE_imm_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Rtype_ABSOLUTE_rs2_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Ftype_ABSOLUTE_sft_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Itype_ABSOLUTE_imm_to32AndHi(uint64_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 64, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Utype_ABSOLUTE_imm_to32AndHi(uint64_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 64, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Stype_ABSOLUTE_imm_to32AndHi(uint64_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 64, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Btype_ABSOLUTE_imm_to32AndHi(uint64_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 64, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Jtype_ABSOLUTE_imm_to32AndHi(uint64_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 64, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Rtype_ABSOLUTE_rs2_to32AndHi(uint64_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 64, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Ftype_ABSOLUTE_sft_to32AndHi(uint64_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 64, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Itype_ABSOLUTE_imm_to32AndLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 12), 12);
        }
        int64_t RV3264I_Utype_ABSOLUTE_imm_to32AndLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 12), 12);
        }
        int64_t RV3264I_Stype_ABSOLUTE_imm_to32AndLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 12), 12);
        }
        int64_t RV3264I_Btype_ABSOLUTE_imm_to32AndLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 12), 12);
        }
        int64_t RV3264I_Jtype_ABSOLUTE_imm_to32AndLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 12), 12);
        }
        int64_t RV3264I_Rtype_ABSOLUTE_rs2_to32AndLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 12), 12);
        }
        int64_t RV3264I_Ftype_ABSOLUTE_sft_to32AndLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x20 ), 6), 12), 12);
        }
        int64_t RV3264I_Itype_ABSOLUTE_imm_lowerHalfHi(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x34 ), 6), 12), 12);
        }
        int64_t RV3264I_Utype_ABSOLUTE_imm_lowerHalfHi(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x34 ), 6), 12), 12);
        }
        int64_t RV3264I_Stype_ABSOLUTE_imm_lowerHalfHi(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x34 ), 6), 12), 12);
        }
        int64_t RV3264I_Btype_ABSOLUTE_imm_lowerHalfHi(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x34 ), 6), 12), 12);
        }
        int64_t RV3264I_Jtype_ABSOLUTE_imm_lowerHalfHi(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x34 ), 6), 12), 12);
        }
        int64_t RV3264I_Rtype_ABSOLUTE_rs2_lowerHalfHi(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x34 ), 6), 12), 12);
        }
        int64_t RV3264I_Ftype_ABSOLUTE_sft_lowerHalfHi(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(VADL_lsr(symbol, 64, ((uint8_t) 0x34 ), 6), 12), 12);
        }
        int64_t RV3264I_Itype_ABSOLUTE_imm_lowerHalfLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Utype_ABSOLUTE_imm_lowerHalfLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Stype_ABSOLUTE_imm_lowerHalfLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Btype_ABSOLUTE_imm_lowerHalfLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Jtype_ABSOLUTE_imm_lowerHalfLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Rtype_ABSOLUTE_rs2_lowerHalfLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Ftype_ABSOLUTE_sft_lowerHalfLo(uint64_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Itype_RELATIVE_imm_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Utype_RELATIVE_imm_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Stype_RELATIVE_imm_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Btype_RELATIVE_imm_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Jtype_RELATIVE_imm_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Rtype_RELATIVE_rs2_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Ftype_RELATIVE_sft_pcrel_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Itype_RELATIVE_imm_pcrel_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Utype_RELATIVE_imm_pcrel_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Stype_RELATIVE_imm_pcrel_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Btype_RELATIVE_imm_pcrel_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Jtype_RELATIVE_imm_pcrel_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Rtype_RELATIVE_rs2_pcrel_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Ftype_RELATIVE_sft_pcrel_lo(uint32_t symbol) {
           return  VADL_sextract(VADL_uextract(symbol, 12), 12);
        }
        int64_t RV3264I_Itype_GLOBAL_OFFSET_TABLE_imm_got_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Utype_GLOBAL_OFFSET_TABLE_imm_got_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Stype_GLOBAL_OFFSET_TABLE_imm_got_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Btype_GLOBAL_OFFSET_TABLE_imm_got_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Jtype_GLOBAL_OFFSET_TABLE_imm_got_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Rtype_GLOBAL_OFFSET_TABLE_rs2_got_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Ftype_GLOBAL_OFFSET_TABLE_sft_got_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int64_t RV3264I_Itype_ABSOLUTE_imm_Itype_ABSOLUTE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Itype_RELATIVE_imm_Itype_RELATIVE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Utype_ABSOLUTE_imm_Utype_ABSOLUTE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Utype_RELATIVE_imm_Utype_RELATIVE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Stype_ABSOLUTE_imm_Stype_ABSOLUTE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Stype_RELATIVE_imm_Stype_RELATIVE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Btype_ABSOLUTE_imm_Btype_ABSOLUTE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Btype_RELATIVE_imm_Btype_RELATIVE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Jtype_ABSOLUTE_imm_Jtype_ABSOLUTE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Jtype_RELATIVE_imm_Jtype_RELATIVE_imm(int64_t input) {
           return input;
        }
        int64_t RV3264I_Rtype_ABSOLUTE_rs2_Rtype_ABSOLUTE_rs2(int64_t input) {
           return input;
        }
        int64_t RV3264I_Rtype_RELATIVE_rs2_Rtype_RELATIVE_rs2(int64_t input) {
           return input;
        }
        int64_t RV3264I_Ftype_ABSOLUTE_sft_Ftype_ABSOLUTE_sft(int64_t input) {
           return input;
        }
        int64_t RV3264I_Ftype_RELATIVE_sft_Ftype_RELATIVE_sft(int64_t input) {
           return input;
        }
        """.trim().lines(), output);
  }
}
