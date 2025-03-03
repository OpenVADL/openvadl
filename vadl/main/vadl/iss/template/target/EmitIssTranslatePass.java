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

package vadl.iss.template.target;

import static vadl.error.Diagnostic.error;

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.IssTranslateCodeGenerator;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/translate.c that contains the functions to generate
 * the TCG instructions from decoded guest instructions.
 * It also contains the {@code gen_intermediate_code} function, called by QEMU as
 * entry point to start the TCG generation.
 */
public class EmitIssTranslatePass extends IssTemplateRenderingPass {
  public EmitIssTranslatePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/translate.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    vars.put("insn_width", getInstructionWidth(specification));
    vars.put("mem_word_size", getMemoryWordSize(specification));
    vars.put("translate_functions",
        getTranslateFunctions(specification));
    return vars;
  }

  /**
   * Gets translate functions.
   */
  private List<String> getTranslateFunctions(Specification specification) {
    var insns = specification.isa().get().ownInstructions();

    return insns.stream()
        .map(i -> IssTranslateCodeGenerator.fetch(i, this.configuration()))
        .toList();
  }

  private static Map<String, Object> getMemoryWordSize(Specification specification) {
    var wordSize = specification.isa().get().ownMemories().get(0).wordSize();
    return Map.of(
        "int", wordSize
    );
  }

  private static Map<String, Object> getInstructionWidth(Specification specification) {
    var refFormat = specification.isa().get().ownInstructions()
        .get(0).format();
    var width = refFormat.type().bitWidth();

    return switch (width) {
      case 8 -> Map.of(
          "short", "b",
          "int", 8
      );
      case 16 -> Map.of(
          "short", "uw",
          "int", 16
      );
      case 32 -> Map.of(
          "short", "l",
          "int", 32
      );
      case 64 -> Map.of(
          "short", "q",
          "int", 64
      );
      default -> throw error("Invalid instruction width", refFormat.identifier.sourceLocation())
          .description(
              "The ISS generator requires that every instruction width "
                  + "is one of [8, 16, 32, 64], but found %s",
              width)
          .build();
    };
  }

}
