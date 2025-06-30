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

package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.gcb.passes.operands.model.TableGenDefaultInstructionOperand;
import vadl.gcb.passes.operands.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionLabelOperand;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.AssemblyDescription;
import vadl.viam.Specification;

/**
 * This file contains the implementation for parsing assembly files.
 */
public class EmitAsmParserCppFilePass extends LcbTemplateRenderingPass {

  public EmitAsmParserCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmParser.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/AsmParser/" + processorName
        + "AsmParser.cpp";
  }

  record AliasDirective(String alias, String target) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "alias", alias,
          "target", target
      );
    }
  }

  record TableGenOperand(String name,
                         int index,
                         boolean requiresPredicate,
                         String predicateMethod) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("name", name,
          "index", index,
          "requiresPredicate", requiresPredicate,
          "predicateMethod", predicateMethod);
    }
  }

  record ParseInstruction(String name,
                          List<TableGenOperand> operands,
                          int numOperands,
                          String targets)
      implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("name", name,
          "targets", targets,
          "operands", operands,
          "numOperands", numOperands);
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        CommonVarNames.ALIASES, directiveMappings(specification.assemblyDescription()),
        CommonVarNames.INSTRUCTIONS, instructions(passResults)
    );
  }

  private List<ParseInstruction> instructions(PassResults passResults) {
    var tableGenMachineInstructions =
        (List<TableGenMachineInstruction>) passResults.lastResultOf(
            GenerateTableGenMachineInstructionRecordPass.class);
    var tableGenPseudoInstructions =
        (List<TableGenPseudoInstruction>) passResults.lastResultOf(
            GenerateTableGenPseudoInstructionRecordPass.class);

    var machine = tableGenMachineInstructions.stream()
        .map(instruction -> {
          var name = instruction.getName();
          var operands = createOperands(instruction);
          int numOperands = operands.size();
          return new ParseInstruction(name,
              operands,
              numOperands,
              operands.stream()
                  .map(x -> "\"" + x.name + "\"")
                  .collect(Collectors.joining(", "))
          );
        })
        .toList();

    var pseudo = tableGenPseudoInstructions.stream()
        .map(instruction -> {
          var name = instruction.getName();
          var operands = createOperands(instruction);
          int numOperands = operands.size();
          return new ParseInstruction(name,
              operands,
              numOperands,
              operands.stream()
                  .map(x -> "\"" + x.name + "\"")
                  .collect(Collectors.joining(", "))
          );
        })
        .toList();

    return Stream.concat(machine.stream(), pseudo.stream()).toList();
  }

  private List<TableGenOperand> createOperands(TableGenInstruction instruction) {
    var result = new ArrayList<TableGenOperand>();
    int indexOffset = 1;
    // Output
    for (var output : instruction.getOutOperands()) {
      var casted = (TableGenDefaultInstructionOperand) output;
      var operand = new TableGenOperand(casted.name(), indexOffset, false, "");
      result.add(operand);
      indexOffset++;
    }

    // Inputs
    for (var input : instruction.getInOperands()) {
      var casted = (TableGenDefaultInstructionOperand) input;
      if (input instanceof TableGenInstructionImmediateOperand immediateOperand) {
        var operand = new TableGenOperand(immediateOperand.name(),
            indexOffset,
            true,
            immediateOperand.immediateOperand().predicateMethod().lower()
        );
        result.add(operand);
      } else if (input instanceof TableGenInstructionLabelOperand immediateOperand) {
        var operand = new TableGenOperand(immediateOperand.name(),
            indexOffset,
            true,
            immediateOperand.immediateOperand().predicateMethod().lower()
        );
        result.add(operand);
      } else {
        var operand = new TableGenOperand(casted.name(),
            indexOffset,
            false,
            ""
        );
        result.add(operand);
      }

      indexOffset++;
    }

    return result;

  }

  private List<TableGenOperand> createOperands(TableGenPseudoInstruction instruction) {
    var result = new ArrayList<TableGenOperand>();
    int indexOffset = 1;

    // Output
    for (var output : instruction.getOutOperands()) {
      var casted = (TableGenDefaultInstructionOperand) output;
      var operand = new TableGenOperand(casted.name(), indexOffset, false, "");
      result.add(operand);
      indexOffset++;
    }

    // Inputs
    for (var input : instruction.getInOperands()) {
      var casted = (TableGenDefaultInstructionOperand) input;
      if (input instanceof TableGenInstructionImmediateOperand immediateOperand) {
        var operand = new TableGenOperand(immediateOperand.name(),
            indexOffset,
            false,
            immediateOperand.immediateOperand().predicateMethod().lower()
        );
        result.add(operand);
      } else if (input instanceof TableGenInstructionLabelOperand immediateOperand) {
        var operand = new TableGenOperand(immediateOperand.name(),
            indexOffset,
            false,
            immediateOperand.immediateOperand().predicateMethod().lower()
        );
        result.add(operand);
      } else {
        var operand = new TableGenOperand(casted.name(),
            indexOffset,
            false,
            ""
        );
        result.add(operand);
      }

      indexOffset++;
    }

    return result;

  }

  private List<AliasDirective> directiveMappings(Optional<AssemblyDescription> asmDescription) {
    return asmDescription.map(
        asmDesc -> asmDesc.directives().stream().map(
            d -> new AliasDirective(d.getAlias(), d.getTarget())).toList()
    ).orElse(List.of());
  }
}
