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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;

public class GenerateLinkerComponentsRiscv32PassTest extends AbstractLcbTest {

  private static Stream<Arguments> modifiers() {
    String input = """
        MO_RV3264Base_hi
        MO_RV3264Base_lo
        MO_RV3264Base_pcrel_hi
        MO_RV3264Base_pcrel_lo
        MO_RV3264Base_got_pcrel_hi
        MO_ABS_RV3264Base_ADDI_immS
        MO_REL_RV3264Base_ADDI_immS
        MO_ABS_RV3264Base_ANDI_immS
        MO_REL_RV3264Base_ANDI_immS
        MO_ABS_RV3264Base_ORI_immS
        MO_REL_RV3264Base_ORI_immS
        MO_ABS_RV3264Base_XORI_immS
        MO_REL_RV3264Base_XORI_immS
        MO_ABS_RV3264Base_SLTI_immS
        MO_REL_RV3264Base_SLTI_immS
        MO_ABS_RV3264Base_SLTIU_immS
        MO_REL_RV3264Base_SLTIU_immS
        MO_ABS_RV3264Base_AUIPC_immUp
        MO_REL_RV3264Base_AUIPC_immUp
        MO_ABS_RV3264Base_LUI_immUp
        MO_REL_RV3264Base_LUI_immUp
        MO_ABS_RV3264Base_LB_immS
        MO_REL_RV3264Base_LB_immS
        MO_ABS_RV3264Base_LBU_immS
        MO_REL_RV3264Base_LBU_immS
        MO_ABS_RV3264Base_LH_immS
        MO_REL_RV3264Base_LH_immS
        MO_ABS_RV3264Base_LHU_immS
        MO_REL_RV3264Base_LHU_immS
        MO_ABS_RV3264Base_LW_immS
        MO_REL_RV3264Base_LW_immS
        MO_ABS_RV3264Base_SB_immS
        MO_REL_RV3264Base_SB_immS
        MO_ABS_RV3264Base_SH_immS
        MO_REL_RV3264Base_SH_immS
        MO_ABS_RV3264Base_SW_immS
        MO_REL_RV3264Base_SW_immS
        MO_ABS_RV3264Base_BEQ_immS
        MO_REL_RV3264Base_BEQ_immS
        MO_ABS_RV3264Base_BNE_immS
        MO_REL_RV3264Base_BNE_immS
        MO_ABS_RV3264Base_BGE_immS
        MO_REL_RV3264Base_BGE_immS
        MO_ABS_RV3264Base_BGEU_immS
        MO_REL_RV3264Base_BGEU_immS
        MO_ABS_RV3264Base_BLT_immS
        MO_REL_RV3264Base_BLT_immS
        MO_ABS_RV3264Base_BLTU_immS
        MO_REL_RV3264Base_BLTU_immS
        MO_ABS_RV3264Base_JAL_immS
        MO_REL_RV3264Base_JAL_immS
        MO_ABS_RV3264Base_JALR_immS
        MO_REL_RV3264Base_JALR_immS
        MO_ABS_RV3264Base_SLLI_shamt
        MO_REL_RV3264Base_SLLI_shamt
        MO_ABS_RV3264Base_SRLI_shamt
        MO_REL_RV3264Base_SRLI_shamt
        MO_ABS_RV3264Base_SRAI_shamt
        MO_REL_RV3264Base_SRAI_shamt
        """;

    return input.lines().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("modifiers")
  void shouldGenerateExpectedModifiers(String name) throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedModifiers = generatedLinkerComponents.modifiers();
    Assertions.assertTrue(
        generatedModifiers.stream().anyMatch(x -> x.value().equals(name.trim())),
        name + " not present");
  }

  private static Stream<Arguments> variantKinds() {
    String input = """
         VK_None
         VK_PLT
         VK_ABS_RV3264Base_hi
         VK_ABS_RV3264Base_lo
         VK_PCREL_RV3264Base_pcrel_hi
         VK_PCREL_RV3264Base_pcrel_lo
         VK_GOT_RV3264Base_got_pcrel_hi
         VK_SYMB_ABS_RV3264Base_ADDI_immS
         VK_SYMB_PCREL_RV3264Base_ADDI_immS
         VK_SYMB_ABS_RV3264Base_ANDI_immS
         VK_SYMB_PCREL_RV3264Base_ANDI_immS
         VK_SYMB_ABS_RV3264Base_ORI_immS
         VK_SYMB_PCREL_RV3264Base_ORI_immS
         VK_SYMB_ABS_RV3264Base_XORI_immS
         VK_SYMB_PCREL_RV3264Base_XORI_immS
         VK_SYMB_ABS_RV3264Base_SLTI_immS
         VK_SYMB_PCREL_RV3264Base_SLTI_immS
         VK_SYMB_ABS_RV3264Base_SLTIU_immS
         VK_SYMB_PCREL_RV3264Base_SLTIU_immS
         VK_SYMB_ABS_RV3264Base_AUIPC_immUp
         VK_SYMB_PCREL_RV3264Base_AUIPC_immUp
         VK_SYMB_ABS_RV3264Base_LUI_immUp
         VK_SYMB_PCREL_RV3264Base_LUI_immUp
         VK_SYMB_ABS_RV3264Base_LB_immS
         VK_SYMB_PCREL_RV3264Base_LB_immS
         VK_SYMB_ABS_RV3264Base_LBU_immS
         VK_SYMB_PCREL_RV3264Base_LBU_immS
         VK_SYMB_ABS_RV3264Base_LH_immS
         VK_SYMB_PCREL_RV3264Base_LH_immS
         VK_SYMB_ABS_RV3264Base_LHU_immS
         VK_SYMB_PCREL_RV3264Base_LHU_immS
         VK_SYMB_ABS_RV3264Base_LW_immS
         VK_SYMB_PCREL_RV3264Base_LW_immS
         VK_SYMB_ABS_RV3264Base_SB_immS
         VK_SYMB_PCREL_RV3264Base_SB_immS
         VK_SYMB_ABS_RV3264Base_SH_immS
         VK_SYMB_PCREL_RV3264Base_SH_immS
         VK_SYMB_ABS_RV3264Base_SW_immS
         VK_SYMB_PCREL_RV3264Base_SW_immS
         VK_SYMB_ABS_RV3264Base_BEQ_immS
         VK_SYMB_PCREL_RV3264Base_BEQ_immS
         VK_SYMB_ABS_RV3264Base_BNE_immS
         VK_SYMB_PCREL_RV3264Base_BNE_immS
         VK_SYMB_ABS_RV3264Base_BGE_immS
         VK_SYMB_PCREL_RV3264Base_BGE_immS
         VK_SYMB_ABS_RV3264Base_BGEU_immS
         VK_SYMB_PCREL_RV3264Base_BGEU_immS
         VK_SYMB_ABS_RV3264Base_BLT_immS
         VK_SYMB_PCREL_RV3264Base_BLT_immS
         VK_SYMB_ABS_RV3264Base_BLTU_immS
         VK_SYMB_PCREL_RV3264Base_BLTU_immS
         VK_SYMB_ABS_RV3264Base_JAL_immS
         VK_SYMB_PCREL_RV3264Base_JAL_immS
         VK_SYMB_ABS_RV3264Base_JALR_immS
         VK_SYMB_PCREL_RV3264Base_JALR_immS
         VK_SYMB_ABS_RV3264Base_SLLI_shamt
         VK_SYMB_PCREL_RV3264Base_SLLI_shamt
         VK_SYMB_ABS_RV3264Base_SRLI_shamt
         VK_SYMB_PCREL_RV3264Base_SRLI_shamt
         VK_SYMB_ABS_RV3264Base_SRAI_shamt
         VK_SYMB_PCREL_RV3264Base_SRAI_shamt
         VK_DECODE_RV3264Base_ADDI_immS
         VK_DECODE_RV3264Base_ANDI_immS
         VK_DECODE_RV3264Base_ORI_immS
         VK_DECODE_RV3264Base_XORI_immS
         VK_DECODE_RV3264Base_SLTI_immS
         VK_DECODE_RV3264Base_SLTIU_immS
         VK_DECODE_RV3264Base_AUIPC_immUp
         VK_DECODE_RV3264Base_LUI_immUp
         VK_DECODE_RV3264Base_LB_immS
         VK_DECODE_RV3264Base_LBU_immS
         VK_DECODE_RV3264Base_LH_immS
         VK_DECODE_RV3264Base_LHU_immS
         VK_DECODE_RV3264Base_LW_immS
         VK_DECODE_RV3264Base_SB_immS
         VK_DECODE_RV3264Base_SH_immS
         VK_DECODE_RV3264Base_SW_immS
         VK_DECODE_RV3264Base_BEQ_immS
         VK_DECODE_RV3264Base_BNE_immS
         VK_DECODE_RV3264Base_BGE_immS
         VK_DECODE_RV3264Base_BGEU_immS
         VK_DECODE_RV3264Base_BLT_immS
         VK_DECODE_RV3264Base_BLTU_immS
         VK_DECODE_RV3264Base_JAL_immS
         VK_DECODE_RV3264Base_JALR_immS
         VK_DECODE_RV3264Base_SLLI_shamt
         VK_DECODE_RV3264Base_SRLI_shamt
         VK_DECODE_RV3264Base_SRAI_shamt
         VK_Invalid
        """;

    return input.lines().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("variantKinds")
  void shouldGenerateExpectedVariantKinds(String variantKindValue)
      throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedVariantKinds = generatedLinkerComponents.variantKinds()
        .stream()
        .map(VariantKind::value)
        .collect(Collectors.toSet());

    Assertions.assertTrue(generatedVariantKinds.contains(variantKindValue.trim()),
        "does not have variant kind: " + variantKindValue);
  }

  private static Stream<Arguments> fixups() {
    String input = """
        fixup_hi_RV3264Base_Jtype_ABSOLUTE_imm
        fixup_hi_RV3264Base_Btype_ABSOLUTE_imm
        fixup_hi_RV3264Base_Itype_ABSOLUTE_imm
        fixup_hi_RV3264Base_Utype_ABSOLUTE_imm
        fixup_hi_RV3264Base_Stype_ABSOLUTE_imm
        fixup_hi_RV3264Base_Ftype_ABSOLUTE_sft
        fixup_lo_RV3264Base_Jtype_ABSOLUTE_imm
        fixup_lo_RV3264Base_Btype_ABSOLUTE_imm
        fixup_lo_RV3264Base_Itype_ABSOLUTE_imm
        fixup_lo_RV3264Base_Utype_ABSOLUTE_imm
        fixup_lo_RV3264Base_Stype_ABSOLUTE_imm
        fixup_lo_RV3264Base_Ftype_ABSOLUTE_sft
        fixup_pcrel_hi_RV3264Base_Jtype_RELATIVE_imm\s
        fixup_pcrel_hi_RV3264Base_Btype_RELATIVE_imm\s
        fixup_pcrel_hi_RV3264Base_Itype_RELATIVE_imm\s
        fixup_pcrel_hi_RV3264Base_Utype_RELATIVE_imm\s
        fixup_pcrel_hi_RV3264Base_Stype_RELATIVE_imm\s
        fixup_pcrel_hi_RV3264Base_Ftype_RELATIVE_sft\s
        fixup_pcrel_lo_RV3264Base_Jtype_RELATIVE_imm\s
        fixup_pcrel_lo_RV3264Base_Btype_RELATIVE_imm\s
        fixup_pcrel_lo_RV3264Base_Itype_RELATIVE_imm\s
        fixup_pcrel_lo_RV3264Base_Utype_RELATIVE_imm\s
        fixup_pcrel_lo_RV3264Base_Stype_RELATIVE_imm\s
        fixup_pcrel_lo_RV3264Base_Ftype_RELATIVE_sft\s
        fixup_got_pcrel_hi_RV3264Base_Jtype_GLOBAL_OFFSET_TABLE_imm\s
        fixup_got_pcrel_hi_RV3264Base_Btype_GLOBAL_OFFSET_TABLE_imm\s
        fixup_got_pcrel_hi_RV3264Base_Itype_GLOBAL_OFFSET_TABLE_imm\s
        fixup_got_pcrel_hi_RV3264Base_Utype_GLOBAL_OFFSET_TABLE_imm\s
        fixup_got_pcrel_hi_RV3264Base_Stype_GLOBAL_OFFSET_TABLE_imm\s
        fixup_got_pcrel_hi_RV3264Base_Ftype_GLOBAL_OFFSET_TABLE_sft\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_ADDI_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_ADDI_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_ANDI_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_ANDI_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_ORI_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_ORI_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_XORI_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_XORI_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_SLTI_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_SLTI_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_SLTIU_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_SLTIU_immS\s
        fixup_immUp_RV3264Base_Utype_ABSOLUTE_AUIPC_immUp\s
        fixup_immUp_RV3264Base_Utype_RELATIVE_AUIPC_immUp\s
        fixup_immUp_RV3264Base_Utype_ABSOLUTE_LUI_immUp\s
        fixup_immUp_RV3264Base_Utype_RELATIVE_LUI_immUp\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_LB_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_LB_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_LBU_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_LBU_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_LH_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_LH_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_LHU_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_LHU_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_LW_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_LW_immS\s
        fixup_immS_RV3264Base_Stype_ABSOLUTE_SB_immS\s
        fixup_immS_RV3264Base_Stype_RELATIVE_SB_immS\s
        fixup_immS_RV3264Base_Stype_ABSOLUTE_SH_immS\s
        fixup_immS_RV3264Base_Stype_RELATIVE_SH_immS\s
        fixup_immS_RV3264Base_Stype_ABSOLUTE_SW_immS\s
        fixup_immS_RV3264Base_Stype_RELATIVE_SW_immS\s
        fixup_immS_RV3264Base_Btype_ABSOLUTE_BEQ_immS\s
        fixup_immS_RV3264Base_Btype_RELATIVE_BEQ_immS\s
        fixup_immS_RV3264Base_Btype_ABSOLUTE_BNE_immS\s
        fixup_immS_RV3264Base_Btype_RELATIVE_BNE_immS\s
        fixup_immS_RV3264Base_Btype_ABSOLUTE_BGE_immS\s
        fixup_immS_RV3264Base_Btype_RELATIVE_BGE_immS\s
        fixup_immS_RV3264Base_Btype_ABSOLUTE_BGEU_immS\s
        fixup_immS_RV3264Base_Btype_RELATIVE_BGEU_immS\s
        fixup_immS_RV3264Base_Btype_ABSOLUTE_BLT_immS\s
        fixup_immS_RV3264Base_Btype_RELATIVE_BLT_immS\s
        fixup_immS_RV3264Base_Btype_ABSOLUTE_BLTU_immS\s
        fixup_immS_RV3264Base_Btype_RELATIVE_BLTU_immS\s
        fixup_immS_RV3264Base_Jtype_ABSOLUTE_JAL_immS\s
        fixup_immS_RV3264Base_Jtype_RELATIVE_JAL_immS\s
        fixup_immS_RV3264Base_Itype_ABSOLUTE_JALR_immS\s
        fixup_immS_RV3264Base_Itype_RELATIVE_JALR_immS\s
        fixup_shamt_RV3264Base_Ftype_ABSOLUTE_SLLI_shamt\s
        fixup_shamt_RV3264Base_Ftype_RELATIVE_SLLI_shamt\s
        fixup_shamt_RV3264Base_Ftype_ABSOLUTE_SRLI_shamt\s
        fixup_shamt_RV3264Base_Ftype_RELATIVE_SRLI_shamt\s
        fixup_shamt_RV3264Base_Ftype_ABSOLUTE_SRAI_shamt\s
        fixup_shamt_RV3264Base_Ftype_RELATIVE_SRAI_shamt
        """;

    return input.lines().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("fixups")
  void shouldGenerateExpectedFixups(String name) throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedFixups = generatedLinkerComponents.fixups()
        .stream().map(x -> x.name().value()).collect(Collectors.toSet());

    Assertions.assertTrue(
        generatedFixups.contains(name.trim()),
        name + " not present");
  }

  private static Stream<Arguments> relocations() {
    String input = """
        R_RV3264Base_Btype_ABSOLUTE_BEQ_immS
        R_RV3264Base_Btype_ABSOLUTE_BGEU_immS
        R_RV3264Base_Btype_ABSOLUTE_BGE_immS
        R_RV3264Base_Btype_ABSOLUTE_BLTU_immS
        R_RV3264Base_Btype_ABSOLUTE_BLT_immS
        R_RV3264Base_Btype_ABSOLUTE_BNE_immS
        R_RV3264Base_Btype_RELATIVE_BEQ_immS
        R_RV3264Base_Btype_RELATIVE_BGEU_immS
        R_RV3264Base_Btype_RELATIVE_BGE_immS
        R_RV3264Base_Btype_RELATIVE_BLTU_immS
        R_RV3264Base_Btype_RELATIVE_BLT_immS
        R_RV3264Base_Btype_RELATIVE_BNE_immS
        R_RV3264Base_Ftype_ABSOLUTE_SLLI_shamt
        R_RV3264Base_Ftype_ABSOLUTE_SRAI_shamt
        R_RV3264Base_Ftype_ABSOLUTE_SRLI_shamt
        R_RV3264Base_Ftype_RELATIVE_SLLI_shamt
        R_RV3264Base_Ftype_RELATIVE_SRAI_shamt
        R_RV3264Base_Ftype_RELATIVE_SRLI_shamt
        R_RV3264Base_Itype_ABSOLUTE_ADDI_immS
        R_RV3264Base_Itype_ABSOLUTE_ANDI_immS
        R_RV3264Base_Itype_ABSOLUTE_JALR_immS
        R_RV3264Base_Itype_ABSOLUTE_LBU_immS
        R_RV3264Base_Itype_ABSOLUTE_LB_immS
        R_RV3264Base_Itype_ABSOLUTE_LHU_immS
        R_RV3264Base_Itype_ABSOLUTE_LH_immS
        R_RV3264Base_Itype_ABSOLUTE_LW_immS
        R_RV3264Base_Itype_ABSOLUTE_ORI_immS
        R_RV3264Base_Itype_ABSOLUTE_SLTIU_immS
        R_RV3264Base_Itype_ABSOLUTE_SLTI_immS
        R_RV3264Base_Itype_ABSOLUTE_XORI_immS
        R_RV3264Base_Itype_RELATIVE_ADDI_immS
        R_RV3264Base_Itype_RELATIVE_ANDI_immS
        R_RV3264Base_Itype_RELATIVE_JALR_immS
        R_RV3264Base_Itype_RELATIVE_LBU_immS
        R_RV3264Base_Itype_RELATIVE_LB_immS
        R_RV3264Base_Itype_RELATIVE_LHU_immS
        R_RV3264Base_Itype_RELATIVE_LH_immS
        R_RV3264Base_Itype_RELATIVE_LW_immS
        R_RV3264Base_Itype_RELATIVE_ORI_immS
        R_RV3264Base_Itype_RELATIVE_SLTIU_immS
        R_RV3264Base_Itype_RELATIVE_SLTI_immS
        R_RV3264Base_Itype_RELATIVE_XORI_immS
        R_RV3264Base_Jtype_ABSOLUTE_JAL_immS
        R_RV3264Base_Jtype_RELATIVE_JAL_immS
        R_RV3264Base_Stype_ABSOLUTE_SB_immS
        R_RV3264Base_Stype_ABSOLUTE_SH_immS
        R_RV3264Base_Stype_ABSOLUTE_SW_immS
        R_RV3264Base_Stype_RELATIVE_SB_immS
        R_RV3264Base_Stype_RELATIVE_SH_immS
        R_RV3264Base_Stype_RELATIVE_SW_immS
        R_RV3264Base_Utype_ABSOLUTE_AUIPC_immUp
        R_RV3264Base_Utype_ABSOLUTE_LUI_immUp
        R_RV3264Base_Utype_RELATIVE_AUIPC_immUp
        R_RV3264Base_Utype_RELATIVE_LUI_immUp
        """;

    return input.lines().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("relocations")
  void shouldGenerateRelocations(String name)
      throws DuplicatedPassKeyException, IOException {
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv32im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));
    var passManager = setup.passManager();

    var generatedLinkerComponents =
        (GenerateLinkerComponentsPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    var generatedRelocations =
        Stream.concat(generatedLinkerComponents.automaticallyGeneratedRelocations().stream(),
                generatedLinkerComponents.userSpecifiedRelocations().stream())
            .map(x -> "R_" + x.identifier().lower())
            .collect(Collectors.toSet());
    Assertions.assertTrue(
        generatedRelocations.contains(name.trim()), name + " not present");
  }
}
