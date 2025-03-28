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

package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.codegen.assembly.AssemblyInstructionPrinterCodeGenerator;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Specification;

/**
 * This file contains the implementation for emitting asm instructions.
 */
public class EmitInstPrinterCppFilePass extends LcbTemplateRenderingPass {

  public EmitInstPrinterCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetInstPrinter.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "InstPrinter.cpp";
  }

  record PrintableInstruction(String name, CppFunctionCode code) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", name,
          "code", code
      );
    }
  }

  record InstructionWithImmediate(Identifier identifier,
                                  String rawEncoderMethod,
                                  Format.FieldAccess fieldAccess,
                                  int opIndex) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "identifier", identifier.simpleName(),
          "rawEncoderMethod", rawEncoderMethod,
          "opIndex", opIndex
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var machineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var supported = machineRecords.stream().map(TableGenMachineInstruction::instruction)
        .collect(Collectors.toSet());
    var tableGenLookup = machineRecords.stream().collect(Collectors.toMap(
        TableGenMachineInstruction::instruction,
        x -> x));
    var printableInstructions = specification
        .isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(supported::contains)
        .map(instruction -> {
          var codeGen = new AssemblyInstructionPrinterCodeGenerator();
          var tableGenRecord =
              ensureNonNull(tableGenLookup.get(instruction), "tablegen record must exist");
          var result = codeGen.generateFunctionBody(instruction, tableGenRecord);
          return new PrintableInstruction(instruction.identifier.simpleName(), result);
        })
        .toList();

    var machineInstructionsWithImmediate = machineRecords
        .stream()
        .filter(x -> x.getInOperands().stream()
            .anyMatch(y -> y instanceof ReferencesImmediateOperand))
        .filter(
            // To indicate that an instruction's immediate needs an encoding,
            // it needs to reference a field access function.
            x -> !x.instruction().assembly().fieldAccesses().isEmpty())
        .flatMap(x -> {
          var fieldAccesses = x.instruction().assembly().fieldAccesses();
          // `operandMappings` is a map from field access to (encoderMethod and OpIndex)
          Map<Format.FieldAccess, Pair<String, Integer>>
              operandMappings = x.getInOperands().stream()
              .filter(y -> y instanceof ReferencesImmediateOperand)
              .collect(Collectors.toMap(
                  y -> ((ReferencesImmediateOperand) y).immediateOperand().fieldAccessRef(),
                  y -> Pair.of(
                      ((ReferencesImmediateOperand) y).immediateOperand().rawEncoderMethod(),
                      x.indexInOperands(y))
              ));

          return fieldAccesses.stream()
              .map(fieldAccess -> {
                var pair = ensureNonNull(operandMappings.get(fieldAccess), "must not be null");
                var encoderMethod = pair.left();
                // Index in the instruction of the immediate which should be encoded
                var opIndex = pair.right();

                return new InstructionWithImmediate(
                    x.instruction().identifier,
                    encoderMethod,
                    fieldAccess,
                    opIndex
                );
              });
        })
        .toList();

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "instructions", printableInstructions,
        "instructionWithEncodedImmediate", machineInstructionsWithImmediate);
  }
}
