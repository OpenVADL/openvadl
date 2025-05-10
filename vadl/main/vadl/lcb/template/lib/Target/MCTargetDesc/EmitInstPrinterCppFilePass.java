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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.lcb.codegen.assembly.AssemblyInstructionPrinterCodeGenerator;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
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

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var machineInstructions = machineInstructions(passResults, specification);
    var pseudoInstructions = pseudoInstructions(passResults, specification);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "instructions",
        Stream.concat(machineInstructions.stream(), pseudoInstructions.stream()).toList());
  }

  @Nonnull
  private List<PrintableInstruction> machineInstructions(PassResults passResults,
                                                         Specification specification) {
    var machineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var supported = machineRecords.stream().map(TableGenMachineInstruction::instruction)
        .collect(Collectors.toSet());
    var tableGenLookup = machineRecords.stream().collect(Collectors.toMap(
        TableGenMachineInstruction::instruction,
        x -> x));
    return specification
        .isa().stream().flatMap(isa -> isa.ownInstructions().stream())
        .filter(supported::contains)
        .map(instruction -> {
          var codeGen = new AssemblyInstructionPrinterCodeGenerator();
          var tableGenRecord =
              ensureNonNull(tableGenLookup.get(instruction), "tablegen record must exist");
          var result = codeGen.generateFunctionBody(instruction, tableGenRecord);
          return new PrintableInstruction(instruction.identifier.simpleName(), result);
        })
        .toList();
  }

  @Nonnull
  private List<PrintableInstruction> pseudoInstructions(PassResults passResults,
                                                        Specification specification) {
    var pseudoRecords = (List<TableGenPseudoInstruction>) passResults.lastResultOf(
        GenerateTableGenPseudoInstructionRecordPass.class);

    var supported = pseudoRecords.stream().map(TableGenPseudoInstruction::pseudoInstruction)
        .collect(Collectors.toSet());

    var tableGenLookup = pseudoRecords.stream().collect(Collectors.toMap(
        TableGenPseudoInstruction::pseudoInstruction,
        x -> x));
    return specification
        .isa().stream().flatMap(isa -> isa.ownPseudoInstructions().stream())
        .filter(supported::contains)
        .map(instruction -> {
          var codeGen = new AssemblyInstructionPrinterCodeGenerator();
          var tableGenRecord =
              ensureNonNull(tableGenLookup.get(instruction), "tablegen record must exist");
          var result = codeGen.generateFunctionBody(instruction, tableGenRecord);
          return new PrintableInstruction(instruction.identifier.simpleName(), result);
        })
        .toList();
  }
}