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

package vadl.lcb.template.include.llvm.BinaryFormat.ELFRelocs;

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
        
        ELF_RELOC(R_RV3264I_hi_Itype_imm, 3)
        ELF_RELOC(R_RV3264I_hi_Utype_imm, 4)
        ELF_RELOC(R_RV3264I_hi_Stype_imm, 5)
        ELF_RELOC(R_RV3264I_hi_Btype_imm, 6)
        ELF_RELOC(R_RV3264I_hi_Jtype_imm, 7)
        ELF_RELOC(R_RV3264I_hi_Ftype_sft, 8)
        ELF_RELOC(R_RV3264I_lo_Itype_imm, 9)
        ELF_RELOC(R_RV3264I_lo_Utype_imm, 10)
        ELF_RELOC(R_RV3264I_lo_Stype_imm, 11)
        ELF_RELOC(R_RV3264I_lo_Btype_imm, 12)
        ELF_RELOC(R_RV3264I_lo_Jtype_imm, 13)
        ELF_RELOC(R_RV3264I_lo_Ftype_sft, 14)
        ELF_RELOC(R_RV3264I_Itype_ABSOLUTE_imm, 15)
        ELF_RELOC(R_RV3264I_Itype_RELATIVE_imm, 16)
        ELF_RELOC(R_RV3264I_Utype_ABSOLUTE_imm, 17)
        ELF_RELOC(R_RV3264I_Utype_RELATIVE_imm, 18)
        ELF_RELOC(R_RV3264I_Stype_ABSOLUTE_imm, 19)
        ELF_RELOC(R_RV3264I_Stype_RELATIVE_imm, 20)
        ELF_RELOC(R_RV3264I_Btype_ABSOLUTE_imm, 21)
        ELF_RELOC(R_RV3264I_Btype_RELATIVE_imm, 22)
        ELF_RELOC(R_RV3264I_Jtype_ABSOLUTE_imm, 23)
        ELF_RELOC(R_RV3264I_Jtype_RELATIVE_imm, 24)
        ELF_RELOC(R_RV3264I_Ftype_ABSOLUTE_sft, 25)
        ELF_RELOC(R_RV3264I_Ftype_RELATIVE_sft, 26)
        """.trim().lines(), output);
  }
}
