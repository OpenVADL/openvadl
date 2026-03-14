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

package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import vadl.error.Diagnostic;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.operands.model.GcbDefaultInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionValueNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.OutputInstructionName;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAddSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadArtificialResourceNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadResourceFactory;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbNodeReplacementHandler;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbNodeReplacementHandlerForMemoryInstructionsReplacement;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.operands.TableGenInstructionImmediateOperand;
import vadl.types.Type;
import vadl.viam.Abi;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * Lowers instructions which can store into memory.
 */
public class LlvmInstructionLoweringMemoryStoreStrategyImpl
    extends LlvmInstructionLoweringFrameIndexHelper {

  public LlvmInstructionLoweringMemoryStoreStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(MachineInstructionLabel.STORE_MEM_WITH_IMMEDIATE);
  }

  @Override
  protected LlvmLoweringPass.Flags getFlags(Graph graph) {
    var flags = super.getFlags(graph);

    return LlvmLoweringPass.Flags.withSideEffects(flags);
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      Graph behavior,
      List<GcbInstructionOperand> inputOperands,
      List<GcbInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      Abi abi) {
    var storesWithoutImmediates = createStoreFromsWithoutImmediate(patterns);
    var truncStores = createSuperRegisterTruncStores(patterns);
    
    var variations = new ArrayList<>(storesWithoutImmediates);
    variations.addAll(truncStores);
    return variations;
  }

  @Override
  protected LcbNodeReplacementHandler getReplacementHandler(PrintableInstruction instruction) {
    return new LcbNodeReplacementHandlerForMemoryInstructionsReplacement(instruction,
        architectureType);
  }

  /**
   * This strategy is ok with it when it has {@link ReadMemNode} or {@link WriteMemNode}.
   */
  @Override
  protected boolean rejectWhenReadingFromMemory(Graph graph) {
    return false;
  }

  /**
   * This strategy is ok with it when it has {@link ReadMemNode} or {@link WriteMemNode}.
   */
  @Override
  protected boolean rejectWhenWritingToMemory(Graph graph) {
    return false;
  }

  private List<TableGenPattern> createSuperRegisterTruncStores(List<TableGenPattern> patterns) {
    return patterns
      .stream()
      .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
      .map(x -> {
        var pattern = (TableGenSelectionWithOutputPattern) x;
        return this.createTruncStoreFromSubRegStore(pattern)
          .or(() -> this.createTruncStoreFromSubRegTruncStore(pattern));
      })
      .flatMap(Optional::stream)
      .toList();
  } 

  private Optional<TableGenPattern> createTruncStoreFromSubRegTruncStore(
    TableGenSelectionWithOutputPattern pattern
  ) {
    return Optional.empty();
  }

  private Optional<TableGenPattern> createTruncStoreFromSubRegStore(
    TableGenSelectionWithOutputPattern pattern
  ) {
      // TODO this is just for testing
      // there are too many assumptions about the form of the graphs
      var selector = pattern.selector().copy();

      var storeNodeOpt = selector
        .getNodes(LlvmStoreSD.class)
        .findFirst()
        .filter(sn -> sn.value() instanceof LlvmReadArtificialResourceNode);
      if(storeNodeOpt.isEmpty()) {
        return Optional.empty();
      }

      // selector
      var storeNode = storeNodeOpt.get();
      var subregNode = (LlvmReadArtificialResourceNode) storeNode.value();
      var subregOperand = (GcbDefaultInstructionOperand) subregNode.operand();
      // TODO
      // what exactly is the fieldRef, and can we really reuse it
      var fieldRef = (FieldRefNode) subregNode.inputs().findFirst().get();
      var baseReg = subregNode.getBaseTensor();

      // TODO can we really make that cast
      var readBaseRegNode = (ReadRegTensorNode) new LlvmReadResourceFactory().create(
          baseReg, fieldRef, baseReg.type().asDataType(), null);
      var truncateNode = new TruncateNode(
        readBaseRegNode,
        subregNode.type());
      var truncStoreNode = new LlvmTruncStore(
        storeNode,
        truncateNode);
      storeNode.replaceAndDelete(truncStoreNode);

      // machine
      // TODO the base reg can be x levels above the subreg
      var machine = pattern.machine().copy();

      String subRegIndexName = CompilerRegister.SubRegIndexEnum.SUB_32.name();
      if(subregNode.type().bitWidth() == 64) {
        subRegIndexName = CompilerRegister.SubRegIndexEnum.FULL_64.name();
      }

      var machineRegisterNode = machine
        .getNodes(LcbMachineInstructionParameterNode.class)
        .filter(x -> x.instructionOperand() instanceof GcbInstructionRegisterFileOperand)
        .filter(x -> ((GcbInstructionRegisterFileOperand) x.instructionOperand()).name().equals(subregOperand.name()))
        .findFirst()
        .get();
      var superMachineRegisterNode = new LcbMachineInstructionParameterNode(
        new GcbInstructionRegisterFileOperand(readBaseRegNode, fieldRef.formatField())
      );
      var extractSubRegNode = new LcbMachineInstructionNode(
          new NodeList<>(superMachineRegisterNode, new ConstantNode(new Constant.Str(subRegIndexName))), 
          new OutputInstructionName("EXTRACT_SUBREG"));
      machineRegisterNode.replaceAndDelete(extractSubRegNode);

      return Optional.of(new TableGenSelectionWithOutputPattern(selector, machine));
  }

  /**
   * LLVM requires a pattern for loading directly from a frame index. But for example in the RISCV
   * specification we only have an instruction which stores from register + immediate. This method
   * will drop the immediate and replace it by {@code 0}.
   */
  private List<TableGenPattern> createStoreFromsWithoutImmediate(List<TableGenPattern> patterns) {
    var alternativePatterns = new ArrayList<TableGenPattern>();

    for (var pattern : patterns.stream()
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x)
        .toList()) {
      var selector = pattern.selector().copy();
      var machine = pattern.machine().copy();

      // Check whether there is an addition with immediate.
      if (selector.getNodes(LlvmAddSD.class).filter(add -> add.arguments().stream()
          .anyMatch(child -> child instanceof LlvmFieldAccessRefNode)).count() == 1) {
        // Yes, so replace the addition with the register which is a child of the addition.
        var addition =
            ensurePresent(selector.getNodes(LlvmAddSD.class).findFirst(),
                "There must be an addition");
        var register = ensurePresent(
            addition.arguments().stream()
                .filter(x -> x instanceof ReadRegTensorNode readRegTensorNode
                    && readRegTensorNode.regTensor().isRegisterFile()).findFirst(),
            () -> Diagnostic.error("Expected a register node as child.",
                addition.location()));

        addition.replaceAndDelete(register);

        // We also have to replace the immediate operand in the machine pattern.
        var immediates = machine.getNodes(LcbMachineInstructionParameterNode.class)
            .filter(x -> x.instructionOperand() instanceof TableGenInstructionImmediateOperand)
            .toList();

        for (var imm : immediates) {
          var ty = ensurePresent(ValueType.from(register.type()),
              () -> Diagnostic.error("Register must have valid llvm type",
                  register.location()));
          imm.replaceAndDelete(new LcbMachineInstructionValueNode(ty, Constant.Value.of(0,
              Type.signedInt(32))));
        }

        alternativePatterns.add(new TableGenSelectionWithOutputPattern(selector, machine));
      }
    }

    return alternativePatterns;
  }
}
