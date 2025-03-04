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

package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.compensation.CompensationPatternPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenImmediateOperandRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstAliasRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstructionPatternRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstructionRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenPseudoInstExpansionRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstExpansionPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;

/**
 * This file contains the mapping for ISelNodes to MI.
 */
public class EmitInstrInfoTableGenFilePass extends LcbTemplateRenderingPass {

  public EmitInstrInfoTableGenFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/InstrInfo.td";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "InstrInfo.td";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var tableGenMachineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var llvmLoweringPassResults =
        (LlvmLoweringPass.LlvmLoweringPassResult) passResults.lastResultOf(
            LlvmLoweringPass.class);
    var labelledMachineInstructions = ((IsaMachineInstructionMatchingPass.Result)
        passResults.lastResultOf(IsaMachineInstructionMatchingPass.class)).labels();
    var tableGenPseudoRecords = (List<TableGenPseudoInstruction>) passResults.lastResultOf(
        GenerateTableGenPseudoInstructionRecordPass.class);

    var addi32 = labelledMachineInstructions.get(MachineInstructionLabel.ADDI_32);
    var addi64 = labelledMachineInstructions.get(MachineInstructionLabel.ADDI_64);
    var luiRaw =
        Objects.requireNonNull(labelledMachineInstructions.get(MachineInstructionLabel.LUI));
    var lui = ensurePresent(luiRaw.stream().findFirst(),
        () -> Diagnostic.error("There must be a load upper immediate instruction",
            specification.sourceLocation()));
    var rawAddi = addi64 != null ? addi64 : Objects.requireNonNull(addi32);

    var addi = ensurePresent(rawAddi.stream().findFirst(),
        () -> Diagnostic.error("Instruction set requires an addition with immediate",
            specification.sourceLocation()));

    var renderedImmediates = ((List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class))
        .stream()
        .map(TableGenImmediateOperandRenderer::lower)
        .toList();

    var renderedTableGenMachineRecords = tableGenMachineRecords
        .stream()
        .map(TableGenInstructionRenderer::lower)
        .toList();

    var renderedTableGenPseudoRecords = tableGenPseudoRecords
        .stream()
        .map(TableGenInstructionRenderer::lower)
        .toList();

    var renderedTableGenInstAliases = llvmLoweringPassResults
        .pseudoInstructionRecords()
        .values()
        .stream()
        .map(TableGenInstAliasRenderer::lower)
        .toList();

    var pseudoExpansionPatterns = tableGenMachineRecords
        .stream()
        .flatMap(x -> x.getAnonymousPatterns().stream())
        .filter(x -> x instanceof TableGenPseudoInstExpansionPattern)
        .map(x -> (TableGenPseudoInstExpansionPattern) x)
        .toList();

    var compensationPatterns =
        (List<TableGenSelectionWithOutputPattern>) passResults.lastResultOf(
            CompensationPatternPass.class);

    var renderedPatterns =
        Stream.concat(
                pseudoExpansionPatterns.stream().map(TableGenPseudoInstExpansionRenderer::lower),
                Stream.concat(
                    tableGenMachineRecords.stream().map(TableGenInstructionPatternRenderer::lower),
                    Stream.concat(
                        tableGenPseudoRecords.stream()
                            .map(TableGenInstructionPatternRenderer::lower),
                        compensationPatterns.stream()
                            .map(TableGenInstructionPatternRenderer::lower))
                ))
            .toList();

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase());
    map.put("returnAddress", abi.returnAddress().render());
    map.put("addi", addi.simpleName());
    map.put("lui", lui.simpleName());
    map.put("stackPointerRegister", abi.stackPointer().render());
    map.put("stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).get().getLlvmType());
    map.put("immediates", renderedImmediates);
    map.put("instructions", renderedTableGenMachineRecords);
    map.put("pseudos", renderedTableGenPseudoRecords);
    map.put("instAliases", renderedTableGenInstAliases);
    map.put("patterns", renderedPatterns);
    map.put("registerFiles", specification.registerFiles().map(this::map).toList());
    return map;
  }

  private Map<String, Object> map(RegisterFile obj) {
    return Map.of(
        "resultWidth", obj.resultType().bitWidth(),
        "name", obj.simpleName()
    );
  }
}