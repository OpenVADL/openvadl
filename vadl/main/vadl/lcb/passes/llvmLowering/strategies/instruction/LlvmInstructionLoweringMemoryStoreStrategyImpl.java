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
import vadl.types.DataType;
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
      ValueType architectureType, ValueType smallestRegisterClassType) {
    super(architectureType, smallestRegisterClassType);
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

    var allPatterns = new ArrayList<>(storesWithoutImmediates);
    allPatterns.addAll(patterns);
    var truncStores = createSuperRegisterTruncStores(allPatterns);
    
    var variations = new ArrayList<>(storesWithoutImmediates);
    variations.addAll(truncStores);
    return variations;
  }

  @Override
  protected LcbNodeReplacementHandler getReplacementHandler(PrintableInstruction instruction) {
    return new LcbNodeReplacementHandlerForMemoryInstructionsReplacement(instruction,
        architectureType, this.smallestRegisterClassType);
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

  /**
   * Given a list of possible 'store' patterns using sub-registers as values, create identical 
   * truncating-stores for the relevant base-registers.
   */
  private List<TableGenPattern> createSuperRegisterTruncStores(List<TableGenPattern> patterns) {
    return patterns
      .stream()
      .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
      .map(x -> {
        var pattern = (TableGenSelectionWithOutputPattern) x;
        return this.createTruncStoreFromSubregStore(pattern)
          .or(() -> this.createTruncStoreFromSubregTruncStore(pattern));
      })
      .flatMap(Optional::stream)
      .toList();
  }

  /**
   * Given a possible truncating store pattern using a sub-register as value, create an identical 
   * truncating store pattern for the sub-registers' base-register.
   * This method will 
   * - replace the sub-register node in the selector pattern with the appropriate base-register
   * - replace the sub-register node in the machine pattern with an LLVM "EXTRACT_SUBREG" node,
   *   using the base-register as operand
   */
  private Optional<TableGenPattern> createTruncStoreFromSubregTruncStore(
      TableGenSelectionWithOutputPattern pattern
  ) {
    var selector = pattern.selector().copy();
  
    var truncStores = selector
        .getNodes(LlvmTruncStore.class)
        .filter(sn -> sn.value() instanceof LlvmReadArtificialResourceNode)
        .toList();
    if (truncStores.size() != 1) {
      return Optional.empty();
    }

    var truncStoreNode = truncStores.getFirst();
    var subRegNode = (LlvmReadArtificialResourceNode) truncStoreNode.value();

    if (!(subRegNode.operand() instanceof GcbDefaultInstructionOperand)) {
      return Optional.empty();
    }

    var fieldRefs = subRegNode.input(FieldRefNode.class).toList();
    if (fieldRefs.size() != 1) {
      return Optional.empty();
    }
    var fieldRef = (FieldRefNode) fieldRefs.getFirst();

    var baseReg = subRegNode.getBaseTensor();
    var readBaseRegNode = (ReadRegTensorNode) new LlvmReadResourceFactory().create(
        baseReg, fieldRef, baseReg.type().asDataType(), null);
    subRegNode.replaceAndDelete(readBaseRegNode);

    var subRegOperandName = ((GcbDefaultInstructionOperand) subRegNode.operand()).name();
    var machine = this.replaceSubregOperandWithSubregExtraction(
        pattern.machine(), 
        readBaseRegNode, 
        fieldRef, 
        subRegOperandName, 
        subRegNode.type().bitWidth());

    return Optional.of(new TableGenSelectionWithOutputPattern(selector, machine));
  }

  /**
   * Given a possible store pattern using a sub-register as value, create an identical truncating
   * store pattern for the sub-registers' base-register.
   * This method will 
   * - replace the sub-register node in the selector pattern with the appropiate base-register
   * - replace the "LlvmStoreSD" with a "LlvmTruncStore" node
   * - replace the sub-register node in the machine pattern with an LLVM "EXTRACT_SUBREG" node,
   *   using the base-register as operand
   */
  private Optional<TableGenPattern> createTruncStoreFromSubregStore(
      TableGenSelectionWithOutputPattern pattern
  ) {
    var selector = pattern.selector().copy();

    var storeNodes = selector
        .getNodes(LlvmStoreSD.class)
        .filter(sn -> sn.value() instanceof LlvmReadArtificialResourceNode)
        .toList();
    if (storeNodes.size() != 1) {
      return Optional.empty();
    }

    var storeNode = storeNodes.getFirst();
    var subRegNode = (LlvmReadArtificialResourceNode) storeNode.value();

    if (!(subRegNode.operand() instanceof GcbDefaultInstructionOperand)) {
      return Optional.empty();
    }

    var fieldRefs = subRegNode.input(FieldRefNode.class).toList();
    if (fieldRefs.size() != 1) {
      return Optional.empty();
    }
    var fieldRef = (FieldRefNode) fieldRefs.getFirst();

    var baseReg = subRegNode.getBaseTensor();
    var readBaseRegNode = (ReadRegTensorNode) new LlvmReadResourceFactory().create(
        baseReg, fieldRef, baseReg.type().asDataType(), null);
    var truncateNode = new TruncateNode(
        readBaseRegNode,
        subRegNode.type());
    var truncStoreNode = new LlvmTruncStore(
        storeNode,
        truncateNode);
    storeNode.replaceAndDelete(truncStoreNode);

    var subRegOperand = (GcbDefaultInstructionOperand) subRegNode.operand();
    var machine = this.replaceSubregOperandWithSubregExtraction(
        pattern.machine(), 
        readBaseRegNode, 
        fieldRef, 
        subRegOperand.name(), 
        subRegNode.type().bitWidth());

    return Optional.of(new TableGenSelectionWithOutputPattern(selector, machine));
  }

  /**
   * Replace the sub-register-operand with the given name with an operand extracting the 
   * sub-register from the given base-register-node, using LLVMs 'EXTRACT_SUBREG'.
   */
  private Graph replaceSubregOperandWithSubregExtraction(
      Graph machine,
      ReadRegTensorNode baseRegisterNode,
      FieldRefNode baseRegisterAddress,
      String subRegOperandName,
      int subRegBitWidth
  ) {
    machine = machine.copy();

    String subRegIndexName = subRegBitWidth == 64 
        ? CompilerRegister.SubRegIndexEnum.FULL_64.name()
        : CompilerRegister.SubRegIndexEnum.SUB_32.name();

    var subRegNode = ensurePresent(
        machine
          .getNodes(LcbMachineInstructionParameterNode.class)
          .filter(x -> {
            var operand = x.instructionOperand();
            return operand instanceof GcbInstructionRegisterFileOperand op 
              && op.name().equals(subRegOperandName);
          })
          .findFirst(), 
        () -> Diagnostic.error(
          "Expected operand from selector-pattern to be present in machine-pattern.", 
          baseRegisterNode.location()));
    var baseRegParameterNode = new LcbMachineInstructionParameterNode(
        new GcbInstructionRegisterFileOperand(
          baseRegisterNode, 
          baseRegisterAddress.formatField())
    );
    var extractSubRegNode = new LcbMachineInstructionNode(
        new NodeList<>(
            baseRegParameterNode, 
            new ConstantNode(new Constant.Str(subRegIndexName))), 
        new OutputInstructionName("EXTRACT_SUBREG"));

    subRegNode.replaceAndDelete(extractSubRegNode);

    return machine;
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
