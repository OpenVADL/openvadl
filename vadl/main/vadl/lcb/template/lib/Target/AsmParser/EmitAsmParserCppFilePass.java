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
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.ValueRange;
import vadl.gcb.passes.ValueRangeCtx;
import vadl.lcb.passes.EncodeAssemblyImmediateAnnotation;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateLabelOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
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
    var processorName = lcbConfiguration().processorName().value();
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

  private List<Map<String, String>> instructionsWithOperands(PassResults results) {
    var output =
        (LlvmLoweringPass.LlvmLoweringPassResult) results.lastResultOf(LlvmLoweringPass.class);
    var result = new ArrayList<Map<String, String>>();

    output.machineInstructionRecords().forEach(
        (insn, llvmRecord) -> {
          var inputs = llvmRecord.info().inputs().stream()
              .map(i -> ((TableGenParameterTypeAndName) i.parameter()).name());
          var outputs = llvmRecord.info().outputs().stream()
              .map(p -> ((TableGenParameterTypeAndName) p.parameter()).name());

          var operands = Stream.concat(outputs, inputs).map(op -> '"' + op + '"').toList();
          result.add(Map.of(
              "name", insn.simpleName(),
              "operands", String.join(", ", operands)
          ));
        }
    );

    output.pseudoInstructionRecords().forEach(
        (pseudo, llvmRecord) -> {
          var operands = Arrays.stream(pseudo.parameters()).map(p -> '"' + p.simpleName() + '"');
          result.add(Map.of(
                "name", pseudo.simpleName(),
                "operands", String.join(", ", operands.toList()))
          );
        }
    );

    return result;
  }

  private List<Map<String, Object>> immediateConversions(PassResults passResults) {
    var output =
        (LlvmLoweringPass.LlvmLoweringPassResult) passResults.lastResultOf(LlvmLoweringPass.class);
    var result = new ArrayList<Map<String, Object>>();

    output.machineInstructionRecords().forEach(
        (insn, llvmRecord) -> {
          var templateVars = new HashMap<String, Object>();
          var immediateOperands = llvmRecord.info().inputs().stream()
              .filter(i -> i instanceof TableGenInstructionImmediateOperand
                  || i instanceof TableGenInstructionImmediateLabelOperand
              ).toList();
          var emitConversion = immediateOperands.size() == 1;

          templateVars.put("insnName", insn.simpleName());
          templateVars.put("emitConversion", emitConversion);

          if (emitConversion) {
            templateVars.put("needsDecode",
                insn.assembly().hasAnnotation(EncodeAssemblyImmediateAnnotation.class));

            var operand = immediateOperands.get(0);
            var immediateRecord = immediateRecord(operand);

            templateVars.put("operandName",
                ((TableGenParameterTypeAndName) operand.parameter()).name());
            templateVars.put("immediateOperandName", immediateRecord.fullname());

            templateVars.put("decodeMethod", immediateRecord.rawDecoderMethod());
            templateVars.put("predicateMethod", immediateRecord.predicateMethod());

            var valueRange = valueRange(insn);
            templateVars.put("lowestValue", valueRange.lowest());
            templateVars.put("highestValue", valueRange.highest());
          }

          result.add(templateVars);
        }
    );

    return result;
  }

  private TableGenImmediateRecord immediateRecord(TableGenInstructionOperand operand) {
    if (operand instanceof TableGenInstructionImmediateOperand imm) {
      return imm.immediateOperand();
    }
    if (operand instanceof TableGenInstructionImmediateLabelOperand labelImm) {
      return labelImm.immediateOperand();
    }
    throw new IllegalStateException("Unreachable");
  }

  private ValueRange valueRange(Instruction instruction) {
    var ctx = ensureNonNull(instruction.extension(ValueRangeCtx.class),
        () -> Diagnostic.error("Has no extension value range", instruction.sourceLocation()));
    return ensurePresent(ctx.getFirst(),
        () -> Diagnostic.error("Has no value range", instruction.sourceLocation()));
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
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
