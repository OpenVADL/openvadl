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

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.ValueRange;
import vadl.gcb.passes.ValueRangeCtx;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenDefaultInstructionOperand;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.AssemblyDescription;
import vadl.viam.Instruction;
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
      String encodeMethod,
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
          "encodeMethod", encodeMethod,
          "decodeMethod", decodeMethod,
          "predicateMethod", predicateMethod,
          "lowestValue", lowestValue,
          "highestValue", highestValue,
          "opIndex", opIndex
      );
    }
  }

  /**
   * Immediate conversions are used to generate the {@code ModifyImmediate} method in the parser.
   * {@code ModifyImmediate} fulfills 4 tasks:
   * <ul>
   *   <li>Applies {@code encode} to the parsed immediate if an
   *   access function was referenced in the grammar</li>
   *   <li>Checks if a normalized immediate is in the valid value range</li>
   *   <li>Applies {@code decode} to fit the expectation of {@code MCInst}</li>
   *   <li>Checks if the {@code predicate} holds for the immediate value</li>
   * </ul>
   */
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
                    immediateOperand.rawEncoderMethod().lower(),
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

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        CommonVarNames.ALIASES, directiveMappings(specification.assemblyDescription()),
        CommonVarNames.INSTRUCTIONS, instructionsWithOperands(passResults),
        "immediateConversions", immediateConversions(passResults)
    );
  }

  private List<AliasDirective> directiveMappings(Optional<AssemblyDescription> asmDescription) {
    return asmDescription.map(
        asmDesc -> asmDesc.directives().stream().map(
            d -> new AliasDirective(d.getAlias(), d.getTarget())).toList()
    ).orElse(List.of());
  }
}
