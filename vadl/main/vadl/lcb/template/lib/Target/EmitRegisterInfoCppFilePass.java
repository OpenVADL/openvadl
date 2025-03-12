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

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.types.SIntType;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.passes.dummyPasses.DummyAbiPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * This file contains the register definitions for compiler backend.
 */
public class EmitRegisterInfoCppFilePass extends LcbTemplateRenderingPass {

  public EmitRegisterInfoCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/RegisterInfo.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName + "RegisterInfo.cpp";
  }

  /**
   * The ADDI and memory manipulation instructions will handle the frame index.
   * Therefore, LLVM requires methods to eliminate the index. An object of this
   * record represents one method for each {@link Instruction} (ADDI, MEM_STORE, MEM_LOAD).
   */
  record FrameIndexElimination(MachineInstructionLabel machineInstructionLabel,
                               Instruction instruction,
                               FieldAccessRefNode immediate,
                               String predicateMethodName,
                               RegisterFile registerFile,
                               MachineInstructionIndices machineInstructionIndices,
                               long minValue,
                               long maxValue) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "minValue", minValue,
          "maxValue", maxValue,
          "predicateMethodName", predicateMethodName,
          "machineInstructionLabel", machineInstructionLabel.name(),
          "machineInstructionIndices", machineInstructionIndices,
          "instruction", instruction.simpleName(),
          "registerFile", registerFile.simpleName()
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi = (Abi) passResults.lastResultOf(DummyAbiPass.class);
    var instructionLabels =
        ((IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class)).labels();
    IdentityHashMap<Instruction, UninlinedGraph> uninlined =
        ((FunctionInlinerPass.Output) passResults
            .lastResultOf(FunctionInlinerPass.class)).behaviors();
    var tableGenMachineInstructions = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var constraints = getConstraints(specification);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "constraints", constraints,
        "framePointer", abi.framePointer().render(),
        "returnAddress", abi.returnAddress().render(),
        "stackPointer", abi.stackPointer().render(),
        "threadPointer", abi.threadPointer().render(),
        "globalPointer", abi.globalPointer().render(),
        "frameIndexEliminations",
        getEliminateFrameIndexEntries(instructionLabels, uninlined,
            tableGenMachineInstructions).stream()
            .sorted(Comparator.comparing(o -> o.instruction.identifier.name())).toList(),
        "registerClasses",
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .toList());
  }

  record ReservedRegister(String registerFile, int index) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "registerFile", registerFile,
          "index", index
      );
    }
  }

  private List<ReservedRegister> getConstraints(Specification specification) {
    var reserved = new ArrayList<ReservedRegister>();
    var registerFiles = specification.registerFiles().toList();

    for (var registerFile : registerFiles) {
      for (var constraint : registerFile.constraints()) {
        reserved.add(
            new ReservedRegister(registerFile.identifier.simpleName(),
                constraint.address().intValue()));
      }
    }

    return reserved;
  }

  /**
   * Stores indices in the machine instruction.
   *
   * @param indexFI          is the frame index in the instruction.
   * @param indexImm         is the index of the immediate in the instruction.
   * @param relativeDistance is the distance between immediate operand and frame index. It is
   *                         easier to store the relative distance because LLVM has utility
   *                         functions to extract the frame index. Otherwise, we would need
   *                         to offset the indexImm by the number of output operands in the
   *                         machine instructions to eliminate the frame index.
   */
  record MachineInstructionIndices(int indexFI, int indexImm, int relativeDistance)
      implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "indexFI", indexFI,
          "indexImm", indexImm,
          "relativeDistance", relativeDistance
      );
    }
  }

  private List<FrameIndexElimination> getEliminateFrameIndexEntries(
      @Nullable Map<MachineInstructionLabel, List<Instruction>> instructionLabels,
      @Nullable IdentityHashMap<Instruction, UninlinedGraph> uninlined,
      List<TableGenMachineInstruction> tableGenMachineInstructions) {
    ensureNonNull(instructionLabels, "labels must exist");
    ensureNonNull(uninlined, "uninlined must exist");

    var entries = new ArrayList<FrameIndexElimination>();
    var affected =
        List.of(MachineInstructionLabel.ADDI_32, MachineInstructionLabel.ADDI_64,
            MachineInstructionLabel.STORE_MEM,
            MachineInstructionLabel.LOAD_MEM);

    for (var label : affected) {
      for (var instruction : instructionLabels.getOrDefault(label, Collections.emptyList())) {
        var behavior = ensureNonNull(uninlined.get(instruction),
            () -> Diagnostic.error("No uninlined behavior was found.",
                instruction.sourceLocation()));
        var immediate = ensurePresent(behavior.getNodes(FieldAccessRefNode.class).findAny(), () ->
            Diagnostic.error("Cannot find an immediate for frame index elimination.",
                instruction.sourceLocation()));
        var indices =
            extractFrameIndexAndImmIndexFromMachineInstruction(tableGenMachineInstructions,
                instruction);
        var isSigned = immediate.fieldAccess().type() instanceof SIntType;
        var fieldBitWidth = immediate.fieldAccess().fieldRef().bitSlice().bitSize();
        long minValue = isSigned ? -1 * (long) Math.pow(2, fieldBitWidth - 1) : 0;
        long maxValue = isSigned ? (long) Math.pow(2, fieldBitWidth - 1) - 1 :
            (long) Math.pow(2, fieldBitWidth);
        var entry = new FrameIndexElimination(label, instruction, immediate,
            immediate.fieldAccess().predicate().identifier.lower(),
            instruction.behavior().getNodes(ReadRegFileNode.class).findFirst().get()
                .registerFile(), indices, minValue, maxValue);
        entries.add(entry);
      }
    }

    return entries;
  }

  private MachineInstructionIndices extractFrameIndexAndImmIndexFromMachineInstruction(
      List<TableGenMachineInstruction> tableGenMachineInstructions, Instruction instruction) {
    var machineInstructionIndices = new ArrayList<MachineInstructionIndices>();

    var record = ensurePresent(
        tableGenMachineInstructions.stream().filter(x -> x.instruction() == instruction)
            .findFirst(),
        "Cannot find a tablegen record for this machine instruction");

    for (var pattern : record.getAnonymousPatterns()) {
      if (pattern instanceof TableGenSelectionWithOutputPattern outputPattern) {
        var rootNode =
            outputPattern.machine().getNodes(LcbMachineInstructionNode.class).findFirst().get();
        var nodeFI = rootNode.arguments().stream()
            .filter(x -> x instanceof LcbMachineInstructionParameterNode)
            .filter(
                x -> ((LcbMachineInstructionParameterNode) x).instructionOperand()
                    instanceof TableGenInstructionFrameRegisterOperand)
            .findFirst();
        var nodeImm = rootNode.arguments().stream()
            .filter(x -> x instanceof LcbMachineInstructionParameterNode)
            .filter(
                x -> ((LcbMachineInstructionParameterNode) x).instructionOperand()
                    instanceof TableGenInstructionImmediateOperand)
            .findFirst();

        if (nodeFI.isPresent() && nodeImm.isPresent()) {
          int indexFI = rootNode.arguments().indexOf(nodeFI.get());
          int indexImm = rootNode.arguments().indexOf(nodeImm.get());

          machineInstructionIndices.add(
              new MachineInstructionIndices(indexFI, indexImm, indexImm - indexFI));
        }
      }
    }

    ensure(!machineInstructionIndices.isEmpty(), "Expected at least one FI pattern");
    return machineInstructionIndices.stream().findFirst().get();
  }
}
