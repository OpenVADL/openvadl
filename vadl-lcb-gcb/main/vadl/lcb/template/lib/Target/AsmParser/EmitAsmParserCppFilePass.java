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
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.RenamedFieldRefNode;
import vadl.gcb.passes.operands.model.GcbDefaultInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionLabelOperand;
import vadl.lcb.passes.operands.TableGenInstructionImmediateOperand;
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
                         boolean requiresPredicate,
                         String predicateMethod,
                         boolean isFieldOperand,
                         String targetName,
                         String decodeMethod,
                         String params) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("name", name,
          "requiresPredicate", requiresPredicate,
          "predicateMethod", predicateMethod,
          "isFieldOperand", isFieldOperand,
          "targetName", targetName,
          "decodeMethod", decodeMethod,
          "params", params);
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
          var targets = targets(instruction);
          int numOperands = numberOfParsedOperands(instruction);
          return new ParseInstruction(name,
              operands,
              numOperands,
              targets.stream()
                  .map(t -> "\"" + t + "\"")
                  .collect(Collectors.joining(", "))
          );
        })
        .toList();

    var pseudo = tableGenPseudoInstructions.stream()
        .map(instruction -> {
          var name = instruction.getName();
          var operands = createOperands(instruction);
          var targets = targets(instruction);
          int numOperands = numberOfParsedOperands(instruction);
          return new ParseInstruction(name,
              operands,
              numOperands,
              targets.stream()
                  .map(t -> "\"" + t + "\"")
                  .collect(Collectors.joining(", "))
          );
        })
        .toList();

    return Stream.concat(machine.stream(), pseudo.stream()).toList();
  }

  private List<String> targets(TableGenInstruction instruction) {
    var targets = new ArrayList<String>();

    for (var output : instruction.getOutOperands()) {
      var casted = (GcbDefaultInstructionOperand) output;
      targets.add(casted.name());
    }

    for (var input : instruction.getInOperands()) {
      var casted = (GcbDefaultInstructionOperand) input;
      targets.add(casted.name());
    }

    return targets;
  }

  private int numberOfParsedOperands(TableGenInstruction instruction) {
    var count = instruction.getOutOperands().size();

    for (var input : instruction.getInOperands()) {
      if (!isRenamedFieldOperand(input)) {
        count++;
      }
    }
    return count;
  }

  private boolean isRenamedFieldOperand(GcbInstructionOperand operand) {
    return operand instanceof GcbInstructionRegisterFileOperand regOperand
        && regOperand.formatField() instanceof RenamedFieldRefNode.RenamedField;
  }

  /**
   * In the AsmParser we need to deal with cases where an operand in the assembly
   * language represents the encoded value of the operand.
   * An example is AUIPC of Risc-V:
   * <pre>
   *   AuipcInstruction @instruction:
   *     mnemonic = "AUIPC" @operand
   *     rd = Register@operand ","
   *     imm = ImmediateOperand
   *   ;
   * </pre>
   * Here the immediate operand is assigned to the field <code>imm</code>
   * as opposed to field access <code>immS</code>, which LLVM actually expects.
   * To meet the expectation we need to call the field access function to transform
   * <code>imm</code> to <code>immS</code>.
   *
   * <p>In the current implementation this method assumes that an immediate operands access function
   * only ever takes one format field as input.
   * Because of this the AsmParser WILL NOT work for architectures where a field access
   * takes multiple fields as input.
   * </p>
   */
  private List<TableGenOperand> createOperands(TableGenInstruction instruction) {
    var result = new ArrayList<TableGenOperand>();
    // Output
    for (var output : instruction.getOutOperands()) {
      var casted = (GcbDefaultInstructionOperand) output;
      var operand = new TableGenOperand(casted.name(), false, "", false, casted.name(), "", "");
      result.add(operand);

      // field access register operands
      if (output instanceof GcbInstructionRegisterFileOperand regOperand) {
        createRegisterFieldAccessOperands(regOperand, casted, result);
      }
    }

    // Inputs
    for (var input : instruction.getInOperands()) {
      var casted = (GcbDefaultInstructionOperand) input;
      if (input instanceof TableGenInstructionImmediateOperand immediateOperand) {

        var formatFields = immediateOperand.formatFields();
        if (formatFields.size() != 1) {
          DeferredDiagnosticStore.add(Diagnostic.warning(
              "The AsmParser cannot deal with access functions with multiple fields.",
              input.origin().location()).build());
        }

        var fieldAccessOperand = new TableGenOperand(immediateOperand.name(),
            true,
            immediateOperand.immediateOperand().predicateMethod().lower(),
            false,
            immediateOperand.name(),
            "",
            ""
        );
        result.add(fieldAccessOperand);

        var fieldOperand = new TableGenOperand(
            formatFields.get(0).simpleName(),
            true,
            immediateOperand.immediateOperand().predicateMethod().lower(),
            true,
            immediateOperand.name(),
            immediateOperand.immediateOperand().rawDecoderMethod().lower(),
            formatFields.stream().map(x -> "opImm64").collect(Collectors.joining(", "))
        );
        result.add(fieldOperand);
      } else if (input instanceof TableGenInstructionLabelOperand immediateOperand) {

        var formatFields = immediateOperand.formatFields();
        if (formatFields.size() != 1) {
          DeferredDiagnosticStore.add(Diagnostic.warning(
              "The AsmParser cannot deal with access functions with multiple fields.",
              input.origin().location()).build());
        }

        var operand = new TableGenOperand(immediateOperand.name(),
            true,
            immediateOperand.immediateOperand().predicateMethod().lower(),
            false,
            immediateOperand.name(),
            "",
            ""
        );
        result.add(operand);

        var fieldOperand = new TableGenOperand(
            formatFields.get(0).simpleName(),
            true,
            immediateOperand.immediateOperand().predicateMethod().lower(),
            true,
            immediateOperand.name(),
            immediateOperand.immediateOperand().rawDecoderMethod().lower(),
            formatFields.stream().map(x -> "opImm64").collect(Collectors.joining(", "))
        );
        result.add(fieldOperand);
      } else if (input instanceof GcbInstructionRegisterFileOperand regOperand) {
        if (regOperand.formatField() instanceof RenamedFieldRefNode.RenamedField renamedField) {
          var operand = new TableGenOperand(
              renamedField.inner().simpleName(),
              false,
              "",
              false,
              casted.name(),
              "",
              ""
          );
          result.add(operand);
        } else {
          // normal register operand
          var operand = new TableGenOperand(casted.name(), false, "", false, casted.name(), "", "");
          result.add(operand);

          // field access register operands
          createRegisterFieldAccessOperands(regOperand, casted, result);
        }

      } else {
        var operand = new TableGenOperand(casted.name(), false, "", false, casted.name(), "", "");
        result.add(operand);
      }

    }

    return result;

  }

  private List<TableGenOperand> createOperands(TableGenPseudoInstruction instruction) {
    var result = new ArrayList<TableGenOperand>();

    // Output
    for (var output : instruction.getOutOperands()) {
      var casted = (GcbDefaultInstructionOperand) output;
      var operand = new TableGenOperand(casted.name(), false, "", false, casted.name(), "", "");
      result.add(operand);
    }

    // Inputs
    for (var input : instruction.getInOperands()) {
      var casted = (GcbDefaultInstructionOperand) input;
      if (input instanceof TableGenInstructionImmediateOperand immediateOperand) {
        var operand =
            new TableGenOperand(immediateOperand.name(), false, "", false, immediateOperand.name(),
                "", "");
        result.add(operand);
      } else if (input instanceof TableGenInstructionLabelOperand immediateOperand) {
        var operand =
            new TableGenOperand(immediateOperand.name(), false, "", false, immediateOperand.name(),
                "", "");
        result.add(operand);
      } else {
        var operand = new TableGenOperand(casted.name(), false, "", false, casted.name(), "", "");
        result.add(operand);
      }
    }

    return result;

  }

  private void createRegisterFieldAccessOperands(
      GcbInstructionRegisterFileOperand regOperand,
      GcbDefaultInstructionOperand casted,
      List<TableGenOperand> result) {
    var fieldAccesses = regOperand.formatField().format().fieldAccesses().stream().filter(
        fa -> fa.fieldRefs().getFirst().simpleName().equals(casted.name())).toList();
    fieldAccesses.forEach(
        fa -> result.add(
            new TableGenOperand(fa.simpleName(), false, "", false, casted.name(), "", ""))
    );
  }

  private List<AliasDirective> directiveMappings(Optional<AssemblyDescription> asmDescription) {
    return asmDescription.map(
        asmDesc -> asmDesc.directives().stream().map(
            d -> new AliasDirective(d.getAlias(), d.getTarget())).toList()
    ).orElse(List.of());
  }
}
