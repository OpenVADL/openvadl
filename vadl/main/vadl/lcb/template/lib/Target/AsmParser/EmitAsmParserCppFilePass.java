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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenDefaultInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
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

  /*
  private List<Map<String, Object>> instructionsWithOperands(PassResults results) {
    var output =
        (LlvmLoweringPass.LlvmLoweringPassResult) results.lastResultOf(LlvmLoweringPass.class);
    var result = new ArrayList<Map<String, Object>>();

    output.machineInstructionRecords().forEach(
        (insn, llvmRecord) -> {

          var operands = llvmRecord.info().outputInputOperands().stream()
              .map(o -> '"' + ((TableGenDefaultInstructionOperand) o).name() + '"')
              .toList();

          var fieldAccesses = new HashMap<String, String>();
          insn.format().fieldAccesses().forEach(
              fieldAccess -> {
                fieldAccesses.put(fieldAccess.simpleName(), fieldAccess.fieldRef().simpleName());
              }
          );

          result.add(Map.of(
              "name", insn.simpleName(),
              "operands", String.join(", ", operands),
              "fieldAccesses", fieldAccesses
          ));
        }
    );

    output.pseudoInstructionRecords().forEach(
        (pseudo, llvmRecord) -> {
          var operands = Arrays.stream(pseudo.parameters()).map(p -> '"' + p.simpleName() + '"');
          result.add(Map.of(
              "name", pseudo.simpleName(),
              "operands", String.join(", ", operands.toList())
          ));
        }
    );

    return result;
  }

  record ImmediateConversion(
      String instructionName,
      String fieldAccessName,
      String operandName,
      String decodeMethod,
      String predicateMethod,
      long lowestValue,
      long highestValue,
      int opIndex
  ) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "insnName", instructionName,
          "fieldAccessName", fieldAccessName,
          "operandName", operandName,
          "decodeMethod", decodeMethod,
          "predicateMethod", predicateMethod,
          "lowestValue", lowestValue,
          "highestValue", highestValue,
          "opIndex", opIndex
      );
    }
  }

  private List<ImmediateConversion> immediateConversions(PassResults passResults) {
    var tableGenMachineInstructions =
        (List<TableGenMachineInstruction>) passResults.lastResultOf(
            GenerateTableGenMachineInstructionRecordPass.class);
    return tableGenMachineInstructions
        .stream()
        .filter(tableGenMachineInstruction -> {
          // We only convert immediates. Therefore, we have to check whether the
          // instruction actually has at least one immediate.
          return !tableGenMachineInstruction.llvmLoweringRecord().info().inputImmediates()
              .isEmpty();
        })
        .flatMap(tableGenMachineInstruction -> {
          var instruction = tableGenMachineInstruction.instruction();
          var valueRange = valueRange(instruction);

          return tableGenMachineInstruction.llvmLoweringRecord().info().inputs().stream()
              .filter(i -> i instanceof ReferencesImmediateOperand)
              .map(tableGenOperand -> {
                var castedTableGenOperand = (TableGenDefaultInstructionOperand) tableGenOperand;
                var immediateOperand =
                    ((ReferencesImmediateOperand) tableGenOperand).immediateOperand();
                var fieldAccess = immediateOperand.fieldAccessRef();
                var opIndex = tableGenMachineInstruction.indexInOperands(tableGenOperand);

                return new ImmediateConversion(
                    instruction.simpleName(),
                    fieldAccess != null ? fieldAccess.simpleName() : "",
                    castedTableGenOperand.name(),
                    immediateOperand.rawDecoderMethod().lower(),
                    immediateOperand.predicateMethod().lower(),
                    valueRange.lowest(),
                    valueRange.highest(),
                    opIndex
                );
              });
        }).toList();
  }

  private ValueRange valueRange(Instruction instruction) {
    var ctx = ensureNonNull(instruction.extension(ValueRangeCtx.class),
        () -> Diagnostic.error("Has no extension value range", instruction.location()));
    return ensurePresent(ctx.getFirst(),
        () -> Diagnostic.error("Has no value range", instruction.location()));
  }
   */

  record TableGenOperand(String name,
                         int index,
                         boolean requiresDecoding,
                         boolean requiresPredicate,
                         String rawDecoderMethod,
                         String predicateMethod) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("name", name,
          "index", index,
          "requiresDecoding", requiresDecoding,
          "requiresPredicate", requiresPredicate,
          "rawDecoderMethod", rawDecoderMethod,
          "predicateMethod", predicateMethod);
    }
  }

  record ParseInstruction(String name, List<TableGenOperand> operands, int numOperands)
      implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("name", name,
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
        CommonVarNames.INSTRUCTIONS, instructions(passResults),
        "immediateConversions", Collections.emptyList()
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
          return new ParseInstruction(name, operands, numOperands);
        })
        .toList();

    var pseudo = tableGenPseudoInstructions.stream()
        .map(instruction -> {
          var name = instruction.getName();
          var operands = createOperands(instruction);
          int numOperands = operands.size();
          return new ParseInstruction(name, operands, numOperands);
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
      var operand = new TableGenOperand(casted.name(), indexOffset, false, false, "", "");
      result.add(operand);
      indexOffset++;
    }

    // Inputs
    for (var input : instruction.getInOperands()) {
      var casted = (TableGenDefaultInstructionOperand) input;
      if (input instanceof TableGenInstructionImmediateOperand immediateOperand) {
        var requiresDecoding = requiresDecoding(instruction, immediateOperand);
        var operand = new TableGenOperand(immediateOperand.name(),
            indexOffset,
            requiresDecoding,
            true,
            immediateOperand.immediateOperand().rawDecoderMethod().lower(),
            immediateOperand.immediateOperand().predicateMethod().lower()
        );
        result.add(operand);
      } else if (input instanceof TableGenInstructionLabelOperand immediateOperand) {
        var requiresDecoding = requiresDecoding(instruction, immediateOperand);
        var operand = new TableGenOperand(immediateOperand.name(),
            indexOffset,
            requiresDecoding,
            true,
            immediateOperand.immediateOperand().rawDecoderMethod().lower(),
            immediateOperand.immediateOperand().predicateMethod().lower()
        );
        result.add(operand);
      } else {
        var operand = new TableGenOperand(casted.name(),
            indexOffset,
            false,
            false,
            "",
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
      var operand = new TableGenOperand(casted.name(), indexOffset, false, false, "", "");
      result.add(operand);
      indexOffset++;
    }

    // Inputs
    for (var input : instruction.getInOperands()) {
      var casted = (TableGenDefaultInstructionOperand) input;
      if (input instanceof TableGenInstructionImmediateOperand immediateOperand) {
        var requiresDecoding = requiresDecoding(instruction, immediateOperand);
        var operand = new TableGenOperand(immediateOperand.name(),
            indexOffset,
            requiresDecoding,
            false,
            immediateOperand.immediateOperand().rawDecoderMethod().lower(),
            immediateOperand.immediateOperand().predicateMethod().lower()
        );
        result.add(operand);
      } else if (input instanceof TableGenInstructionLabelOperand immediateOperand) {
        var requiresDecoding = requiresDecoding(instruction, immediateOperand);
        var operand = new TableGenOperand(immediateOperand.name(),
            indexOffset,
            requiresDecoding,
            false,
            immediateOperand.immediateOperand().rawDecoderMethod().lower(),
            immediateOperand.immediateOperand().predicateMethod().lower()
        );
        result.add(operand);
      } else {
        var operand = new TableGenOperand(casted.name(),
            indexOffset,
            false,
            false,
            "",
            ""
        );
        result.add(operand);
      }

      indexOffset++;
    }

    return result;

  }

  private boolean requiresDecoding(TableGenInstruction instruction,
                                   ReferencesImmediateOperand operand) {
    if (instruction instanceof TableGenMachineInstruction) {
      return false;
    } else if (instruction instanceof TableGenPseudoInstruction) {
      return false;
    }

    return false;
  }

  private List<AliasDirective> directiveMappings(Optional<AssemblyDescription> asmDescription) {
    return asmDescription.map(
        asmDesc -> asmDesc.directives().stream().map(
            d -> new AliasDirective(d.getAlias(), d.getTarget())).toList()
    ).orElse(List.of());
  }
}
