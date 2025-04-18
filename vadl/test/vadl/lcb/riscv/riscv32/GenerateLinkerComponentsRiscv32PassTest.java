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

package vadl.lcb.riscv.riscv32;


import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.gcb.passes.relocation.model.Modifier;
import vadl.gcb.valuetypes.RelocationFunctionLabel;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;

public class GenerateLinkerComponentsRiscv32PassTest extends AbstractLcbTest {

  @Test
  void shouldGenerateExpectedModifiers() throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedModifiers = generatedLinkerComponents.modifiers();
    expectedModifiers().forEach(
        expected -> Assertions.assertTrue(
            generatedModifiers.stream()
                .anyMatch(generated -> generated.value().equals(expected.value())
                    && generated.kind().equals(expected.kind())
                    &&
                    generated.relocationFunctionLabel().equals(expected.relocationFunctionLabel())
                ),
            "Expected modifier: " + expected + " not found in generated modifiers: "
                + generatedModifiers
        )
    );
  }

  private static Stream<Modifier> expectedModifiers() {
    CompilerRelocation.Kind ABS = CompilerRelocation.Kind.ABSOLUTE;
    CompilerRelocation.Kind REL = CompilerRelocation.Kind.RELATIVE;

    return Stream.of(
        new Modifier("MO_RV3264I_hi", ABS, Optional.of(RelocationFunctionLabel.HI)),
        new Modifier("MO_RV3264I_lo", ABS, Optional.of(RelocationFunctionLabel.LO)),
        new Modifier("MO_RV3264I_pcrel_hi", REL, Optional.of(RelocationFunctionLabel.UNKNOWN)),
        new Modifier("MO_RV3264I_pcrel_lo", REL, Optional.of(RelocationFunctionLabel.UNKNOWN)),
        new Modifier("MO_RV3264I_got_hi", REL, Optional.of(RelocationFunctionLabel.UNKNOWN)),
        new Modifier("MO_ABS_RV3264I_Itype_imm", ABS, Optional.empty()),
        new Modifier("MO_REL_RV3264I_Itype_imm", REL, Optional.empty()),
        new Modifier("MO_ABS_RV3264I_Utype_imm", ABS, Optional.empty()),
        new Modifier("MO_REL_RV3264I_Utype_imm", REL, Optional.empty()),
        new Modifier("MO_ABS_RV3264I_Stype_imm", ABS, Optional.empty()),
        new Modifier("MO_REL_RV3264I_Stype_imm", REL, Optional.empty()),
        new Modifier("MO_ABS_RV3264I_Btype_imm", ABS, Optional.empty()),
        new Modifier("MO_REL_RV3264I_Btype_imm", REL, Optional.empty()),
        new Modifier("MO_ABS_RV3264I_Jtype_imm", ABS, Optional.empty()),
        new Modifier("MO_REL_RV3264I_Jtype_imm", REL, Optional.empty()),
        new Modifier("MO_ABS_RV3264I_Ftype_sft", ABS, Optional.empty()),
        new Modifier("MO_REL_RV3264I_Ftype_sft", REL, Optional.empty())
    );
  }

  @Test
  void shouldGenerateExpectedVariantKinds() throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedVariantKinds = generatedLinkerComponents.variantKinds();
    expectedVariantKinds().forEach(
        expected -> Assertions.assertTrue(
            generatedVariantKinds.stream()
                .anyMatch(generated -> generated.value().equals(expected.value())
                    && generated.human().equals(expected.human())
                    && generated.isImmediate() == expected.isImmediate()
                ),
            "Expected variant kind: " + expected + " not found in generated variant kinds: "
                + generatedVariantKinds
        )
    );
  }

  private static Stream<VariantKind> expectedVariantKinds() {
    return Stream.of(
        VariantKind.none(),
        VariantKind.invalid(),
        new VariantKind("VK_ABS_RV3264I_hi", "hi", false),
        new VariantKind("VK_ABS_RV3264I_lo", "lo", false),
        new VariantKind("VK_PCREL_RV3264I_pcrel_hi", "pcrel_hi", false),
        new VariantKind("VK_PCREL_RV3264I_pcrel_lo", "pcrel_lo", false),
        new VariantKind("VK_GOT_RV3264I_got_hi", "got_hi", false),
        new VariantKind("VK_SYMB_ABS_RV3264I_Itype_imm", "SYMB_ABS_RV3264I_Itype_imm", true),
        new VariantKind("VK_SYMB_PCREL_RV3264I_Itype_imm", "SYMB_PCREL_RV3264I_Itype_imm", true),
        new VariantKind("VK_SYMB_ABS_RV3264I_Utype_imm", "SYMB_ABS_RV3264I_Utype_imm", true),
        new VariantKind("VK_SYMB_PCREL_RV3264I_Utype_imm", "SYMB_PCREL_RV3264I_Utype_imm", true),
        new VariantKind("VK_SYMB_ABS_RV3264I_Stype_imm", "SYMB_ABS_RV3264I_Stype_imm", true),
        new VariantKind("VK_SYMB_PCREL_RV3264I_Stype_imm", "SYMB_PCREL_RV3264I_Stype_imm", true),
        new VariantKind("VK_SYMB_ABS_RV3264I_Btype_imm", "SYMB_ABS_RV3264I_Btype_imm", true),
        new VariantKind("VK_SYMB_PCREL_RV3264I_Btype_imm", "SYMB_PCREL_RV3264I_Btype_imm", true),
        new VariantKind("VK_SYMB_ABS_RV3264I_Jtype_imm", "SYMB_ABS_RV3264I_Jtype_imm", true),
        new VariantKind("VK_SYMB_PCREL_RV3264I_Jtype_imm", "SYMB_PCREL_RV3264I_Jtype_imm", true),
        new VariantKind("VK_SYMB_ABS_RV3264I_Ftype_sft", "SYMB_ABS_RV3264I_Ftype_sft", true),
        new VariantKind("VK_SYMB_PCREL_RV3264I_Ftype_sft", "SYMB_PCREL_RV3264I_Ftype_sft", true),
        new VariantKind("VK_DECODE_RV3264I_Rtype_shamt", "DECODE_RV3264I_Rtype_shamt", true),
        new VariantKind("VK_DECODE_RV3264I_Itype_immS", "DECODE_RV3264I_Itype_immS", true),
        new VariantKind("VK_DECODE_RV3264I_Utype_immUp", "DECODE_RV3264I_Utype_immUp", true),
        new VariantKind("VK_DECODE_RV3264I_Stype_immS", "DECODE_RV3264I_Stype_immS", true),
        new VariantKind("VK_DECODE_RV3264I_Btype_immS", "DECODE_RV3264I_Btype_immS", true),
        new VariantKind("VK_DECODE_RV3264I_Jtype_immS", "DECODE_RV3264I_Jtype_immS", true),
        new VariantKind("VK_DECODE_RV3264I_Ftype_shamt", "DECODE_RV3264I_Ftype_shamt", true)
    );
  }

  @Test
  void shouldGenerateExpectedFixups() throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedFixups = generatedLinkerComponents.fixups();
    expectedFixupIds().forEach(
        expected -> Assertions.assertTrue(
            generatedFixups.stream()
                .anyMatch(generated -> generated.name().value().equals(expected)),
            "Expected fixup: " + expected + " not found in generated fixups: "
                + generatedFixups
        )
    );
  }

  private static Stream<String> expectedFixupIds() {
    return Stream.of(
        "fixup_hi_RV3264I_Itype_ABSOLUTE_imm",
        "fixup_hi_RV3264I_Utype_ABSOLUTE_imm",
        "fixup_hi_RV3264I_Stype_ABSOLUTE_imm",
        "fixup_hi_RV3264I_Btype_ABSOLUTE_imm",
        "fixup_hi_RV3264I_Jtype_ABSOLUTE_imm",
        "fixup_hi_RV3264I_Ftype_ABSOLUTE_sft",
        "fixup_lo_RV3264I_Itype_ABSOLUTE_imm",
        "fixup_lo_RV3264I_Utype_ABSOLUTE_imm",
        "fixup_lo_RV3264I_Stype_ABSOLUTE_imm",
        "fixup_lo_RV3264I_Btype_ABSOLUTE_imm",
        "fixup_lo_RV3264I_Jtype_ABSOLUTE_imm",
        "fixup_lo_RV3264I_Ftype_ABSOLUTE_sft",
        "fixup_pcrel_hi_RV3264I_Itype_RELATIVE_imm",
        "fixup_pcrel_hi_RV3264I_Utype_RELATIVE_imm",
        "fixup_pcrel_hi_RV3264I_Stype_RELATIVE_imm",
        "fixup_pcrel_hi_RV3264I_Btype_RELATIVE_imm",
        "fixup_pcrel_hi_RV3264I_Jtype_RELATIVE_imm",
        "fixup_pcrel_hi_RV3264I_Ftype_RELATIVE_sft",
        "fixup_pcrel_lo_RV3264I_Itype_RELATIVE_imm",
        "fixup_pcrel_lo_RV3264I_Utype_RELATIVE_imm",
        "fixup_pcrel_lo_RV3264I_Stype_RELATIVE_imm",
        "fixup_pcrel_lo_RV3264I_Btype_RELATIVE_imm",
        "fixup_pcrel_lo_RV3264I_Jtype_RELATIVE_imm",
        "fixup_pcrel_lo_RV3264I_Ftype_RELATIVE_sft",
        "fixup_got_hi_RV3264I_Itype_GLOBAL_OFFSET_TABLE_imm",
        "fixup_got_hi_RV3264I_Utype_GLOBAL_OFFSET_TABLE_imm",
        "fixup_got_hi_RV3264I_Stype_GLOBAL_OFFSET_TABLE_imm",
        "fixup_got_hi_RV3264I_Btype_GLOBAL_OFFSET_TABLE_imm",
        "fixup_got_hi_RV3264I_Jtype_GLOBAL_OFFSET_TABLE_imm",
        "fixup_got_hi_RV3264I_Ftype_GLOBAL_OFFSET_TABLE_sft",
        "fixup_imm_RV3264I_Itype_ABSOLUTE_imm",
        "fixup_imm_RV3264I_Itype_RELATIVE_imm",
        "fixup_imm_RV3264I_Utype_ABSOLUTE_imm",
        "fixup_imm_RV3264I_Utype_RELATIVE_imm",
        "fixup_imm_RV3264I_Stype_ABSOLUTE_imm",
        "fixup_imm_RV3264I_Stype_RELATIVE_imm",
        "fixup_imm_RV3264I_Btype_ABSOLUTE_imm",
        "fixup_imm_RV3264I_Btype_RELATIVE_imm",
        "fixup_imm_RV3264I_Jtype_ABSOLUTE_imm",
        "fixup_imm_RV3264I_Jtype_RELATIVE_imm",
        "fixup_sft_RV3264I_Ftype_ABSOLUTE_sft",
        "fixup_sft_RV3264I_Ftype_RELATIVE_sft"
    );
  }


  @Test
  void shouldGenerateAutomaticallyGeneratedRelocations()
      throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedRelocations = generatedLinkerComponents.automaticallyGeneratedRelocations();
    expectedAutomaticallyGeneratedRelocationIds().forEach(
        expected -> Assertions.assertTrue(
            generatedRelocations.stream()
                .anyMatch(generated -> generated.elfRelocationName().value().equals(expected)),
            "Expected AutomaticallyGeneratedRelocation: " + expected
                + " not found in generated relocations: " + generatedRelocations
        )
    );
  }

  private static Stream<String> expectedAutomaticallyGeneratedRelocationIds() {
    return Stream.of(
        "R_RV3264I_Itype_ABSOLUTE_imm",
        "R_RV3264I_Itype_RELATIVE_imm",
        "R_RV3264I_Utype_ABSOLUTE_imm",
        "R_RV3264I_Utype_RELATIVE_imm",
        "R_RV3264I_Stype_ABSOLUTE_imm",
        "R_RV3264I_Stype_RELATIVE_imm",
        "R_RV3264I_Btype_ABSOLUTE_imm",
        "R_RV3264I_Btype_RELATIVE_imm",
        "R_RV3264I_Jtype_ABSOLUTE_imm",
        "R_RV3264I_Jtype_RELATIVE_imm",
        "R_RV3264I_Ftype_ABSOLUTE_sft",
        "R_RV3264I_Ftype_RELATIVE_sft"
    );
  }

  @Test
  void shouldGenerateUserSpecifiedRelocations()
      throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedRelocations = generatedLinkerComponents.userSpecifiedRelocations();
    expectedUserSpecifiedRelocationIds().forEach(
        expected -> Assertions.assertTrue(
            generatedRelocations.stream()
                .anyMatch(generated -> generated.elfRelocationName().value().equals(expected)),
            "Expected ImplementedUserSpecifiedRelocation: " + expected
                + " not found in generated relocations: " + generatedRelocations
        )
    );
  }

  private static Stream<String> expectedUserSpecifiedRelocationIds() {
    return Stream.of(
        "R_RV3264I_hi_Itype_imm",
        "R_RV3264I_hi_Utype_imm",
        "R_RV3264I_hi_Stype_imm",
        "R_RV3264I_hi_Btype_imm",
        "R_RV3264I_hi_Jtype_imm",
        "R_RV3264I_hi_Ftype_sft",
        "R_RV3264I_lo_Itype_imm",
        "R_RV3264I_lo_Utype_imm",
        "R_RV3264I_lo_Stype_imm",
        "R_RV3264I_lo_Btype_imm",
        "R_RV3264I_lo_Jtype_imm",
        "R_RV3264I_lo_Ftype_sft",
        "R_RV3264I_pcrel_hi_Itype_imm",
        "R_RV3264I_pcrel_hi_Utype_imm",
        "R_RV3264I_pcrel_hi_Stype_imm",
        "R_RV3264I_pcrel_hi_Btype_imm",
        "R_RV3264I_pcrel_hi_Jtype_imm",
        "R_RV3264I_pcrel_hi_Ftype_sft",
        "R_RV3264I_pcrel_lo_Itype_imm",
        "R_RV3264I_pcrel_lo_Utype_imm",
        "R_RV3264I_pcrel_lo_Stype_imm",
        "R_RV3264I_pcrel_lo_Btype_imm",
        "R_RV3264I_pcrel_lo_Jtype_imm",
        "R_RV3264I_pcrel_lo_Ftype_sft",
        "R_RV3264I_got_hi_Itype_imm",
        "R_RV3264I_got_hi_Utype_imm",
        "R_RV3264I_got_hi_Stype_imm",
        "R_RV3264I_got_hi_Btype_imm",
        "R_RV3264I_got_hi_Jtype_imm",
        "R_RV3264I_got_hi_Ftype_sft"
    );
  }
}
