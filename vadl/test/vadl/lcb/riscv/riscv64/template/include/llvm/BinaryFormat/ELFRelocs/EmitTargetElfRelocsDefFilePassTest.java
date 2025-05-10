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
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitTargetElfRelocsDefFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(
            vadl.lcb.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(
                vadl.lcb.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass.class);

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
        
        ELF_RELOC(R_RV3264Base_hi_Itype_imm, 3)
        ELF_RELOC(R_RV3264Base_hi_Utype_imm, 4)
        ELF_RELOC(R_RV3264Base_hi_Stype_imm, 5)
        ELF_RELOC(R_RV3264Base_hi_Btype_imm, 6)
        ELF_RELOC(R_RV3264Base_hi_Jtype_imm, 7)
        ELF_RELOC(R_RV3264Base_hi_Rtype_rs2, 8)
        ELF_RELOC(R_RV3264Base_hi_Ftype_sft, 9)
        ELF_RELOC(R_RV3264Base_lo_Itype_imm, 10)
        ELF_RELOC(R_RV3264Base_lo_Utype_imm, 11)
        ELF_RELOC(R_RV3264Base_lo_Stype_imm, 12)
        ELF_RELOC(R_RV3264Base_lo_Btype_imm, 13)
        ELF_RELOC(R_RV3264Base_lo_Jtype_imm, 14)
        ELF_RELOC(R_RV3264Base_lo_Rtype_rs2, 15)
        ELF_RELOC(R_RV3264Base_lo_Ftype_sft, 16)
        ELF_RELOC(R_RV3264Base_to32AndHi_Itype_imm, 17)
        ELF_RELOC(R_RV3264Base_to32AndHi_Utype_imm, 18)
        ELF_RELOC(R_RV3264Base_to32AndHi_Stype_imm, 19)
        ELF_RELOC(R_RV3264Base_to32AndHi_Btype_imm, 20)
        ELF_RELOC(R_RV3264Base_to32AndHi_Jtype_imm, 21)
        ELF_RELOC(R_RV3264Base_to32AndHi_Rtype_rs2, 22)
        ELF_RELOC(R_RV3264Base_to32AndHi_Ftype_sft, 23)
        ELF_RELOC(R_RV3264Base_to32AndLo_Itype_imm, 24)
        ELF_RELOC(R_RV3264Base_to32AndLo_Utype_imm, 25)
        ELF_RELOC(R_RV3264Base_to32AndLo_Stype_imm, 26)
        ELF_RELOC(R_RV3264Base_to32AndLo_Btype_imm, 27)
        ELF_RELOC(R_RV3264Base_to32AndLo_Jtype_imm, 28)
        ELF_RELOC(R_RV3264Base_to32AndLo_Rtype_rs2, 29)
        ELF_RELOC(R_RV3264Base_to32AndLo_Ftype_sft, 30)
        ELF_RELOC(R_RV3264Base_lowerHalfHi_Itype_imm, 31)
        ELF_RELOC(R_RV3264Base_lowerHalfHi_Utype_imm, 32)
        ELF_RELOC(R_RV3264Base_lowerHalfHi_Stype_imm, 33)
        ELF_RELOC(R_RV3264Base_lowerHalfHi_Btype_imm, 34)
        ELF_RELOC(R_RV3264Base_lowerHalfHi_Jtype_imm, 35)
        ELF_RELOC(R_RV3264Base_lowerHalfHi_Rtype_rs2, 36)
        ELF_RELOC(R_RV3264Base_lowerHalfHi_Ftype_sft, 37)
        ELF_RELOC(R_RV3264Base_lowerHalfLo_Itype_imm, 38)
        ELF_RELOC(R_RV3264Base_lowerHalfLo_Utype_imm, 39)
        ELF_RELOC(R_RV3264Base_lowerHalfLo_Stype_imm, 40)
        ELF_RELOC(R_RV3264Base_lowerHalfLo_Btype_imm, 41)
        ELF_RELOC(R_RV3264Base_lowerHalfLo_Jtype_imm, 42)
        ELF_RELOC(R_RV3264Base_lowerHalfLo_Rtype_rs2, 43)
        ELF_RELOC(R_RV3264Base_lowerHalfLo_Ftype_sft, 44)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Itype_imm, 45)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Utype_imm, 46)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Stype_imm, 47)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Btype_imm, 48)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Jtype_imm, 49)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Rtype_rs2, 50)
        ELF_RELOC(R_RV3264Base_pcrel_hi_Ftype_sft, 51)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Itype_imm, 52)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Utype_imm, 53)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Stype_imm, 54)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Btype_imm, 55)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Jtype_imm, 56)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Rtype_rs2, 57)
        ELF_RELOC(R_RV3264Base_pcrel_lo_Ftype_sft, 58)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Itype_imm, 59)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Utype_imm, 60)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Stype_imm, 61)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Btype_imm, 62)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Jtype_imm, 63)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Rtype_rs2, 64)
        ELF_RELOC(R_RV3264Base_got_pcrel_hi_Ftype_sft, 65)
        ELF_RELOC(R_RV3264Base_Itype_ABSOLUTE_imm, 66)
        ELF_RELOC(R_RV3264Base_Itype_RELATIVE_imm, 67)
        ELF_RELOC(R_RV3264Base_Utype_ABSOLUTE_imm, 68)
        ELF_RELOC(R_RV3264Base_Utype_RELATIVE_imm, 69)
        ELF_RELOC(R_RV3264Base_Stype_ABSOLUTE_imm, 70)
        ELF_RELOC(R_RV3264Base_Stype_RELATIVE_imm, 71)
        ELF_RELOC(R_RV3264Base_Btype_ABSOLUTE_imm, 72)
        ELF_RELOC(R_RV3264Base_Btype_RELATIVE_imm, 73)
        ELF_RELOC(R_RV3264Base_Jtype_ABSOLUTE_imm, 74)
        ELF_RELOC(R_RV3264Base_Jtype_RELATIVE_imm, 75)
        ELF_RELOC(R_RV3264Base_Rtype_ABSOLUTE_rs2, 76)
        ELF_RELOC(R_RV3264Base_Rtype_RELATIVE_rs2, 77)
        ELF_RELOC(R_RV3264Base_Ftype_ABSOLUTE_sft, 78)
        ELF_RELOC(R_RV3264Base_Ftype_RELATIVE_sft, 79)
        """.trim().lines(), output);
  }
}
