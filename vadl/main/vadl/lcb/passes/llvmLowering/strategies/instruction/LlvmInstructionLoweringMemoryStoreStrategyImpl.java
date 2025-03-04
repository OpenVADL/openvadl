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

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionValueNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAddSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.types.Type;
import vadl.viam.Abi;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.Register;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteResourceNode;

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
    return Set.of(MachineInstructionLabel.STORE_MEM);
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks() {
    return replacementHooksWithDefaultFieldAccessReplacement();
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      Abi abi) {
    var alternativePatterns = new ArrayList<TableGenPattern>();
    var storesWithoutImmediates = createStoreFromsWithoutImmediate(patterns);
    var frameIndexPatterns = replaceRegisterWithFrameIndex(
        Stream.concat(patterns.stream(), storesWithoutImmediates.stream()).toList());

    alternativePatterns.addAll(storesWithoutImmediates);
    alternativePatterns.addAll(frameIndexPatterns);

    return alternativePatterns;
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
            addition.arguments().stream().filter(x -> x instanceof ReadRegFileNode).findFirst(),
            () -> Diagnostic.error("Expected a register node as child.",
                addition.sourceLocation()));

        addition.replaceAndDelete(register);

        // We also have to replace the immediate operand in the machine pattern.
        var immediates = machine.getNodes(LcbMachineInstructionParameterNode.class)
            .filter(x -> x.instructionOperand() instanceof TableGenInstructionImmediateOperand)
            .toList();

        for (var imm : immediates) {
          var ty = ensurePresent(ValueType.from(register.type()),
              () -> Diagnostic.error("Register must have valid llvm type",
                  register.sourceLocation()));
          imm.replaceAndDelete(new LcbMachineInstructionValueNode(ty, Constant.Value.of(0,
              Type.signedInt(32))));
        }

        alternativePatterns.add(new TableGenSelectionWithOutputPattern(selector, machine));
      }
    }

    return alternativePatterns;
  }

  /**
   * Instructions in {@link MachineInstructionLabel#STORE_MEM} write from a {@link Register} into
   * {@link Memory}. However, LLVM has a special selection dag node for frame indexes.
   * Function's variables are placed on the stack and will be accessed relative to a frame pointer.
   * LLVM has for the lowering a frame index leaf node which requires additional patterns.
   * The goal of this method is to replace a {@link Register} with {@link LlvmFrameIndexSD}
   * which has a LLVM's {@code ComplexPattern} hardcoded.
   */
  private List<TableGenPattern> replaceRegisterWithFrameIndex(List<TableGenPattern> patterns) {
    var alternativePatterns = new ArrayList<TableGenPattern>();

    for (var pattern : patterns.stream()
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x)
        .toList()) {
      var selector = pattern.selector().copy();
      var machine = pattern.machine().copy();

      // We are only interested in the `address` subtree of a memory store (or truncstore)
      // because the value register should remain unchanged.
      // Afterward, we get all the children of the `WriteResource` and only filter for
      // `LlvmReadRegFileNode` because we wil only change registers.
      var affectedNodes = selector.getNodes(Set.of(LlvmTruncStore.class, LlvmStoreSD.class))
          .map(x -> (WriteResourceNode) x)
          .filter(WriteResourceNode::hasAddress)
          .flatMap(x -> {
            var inputs = new ArrayList<Node>();
            var address = x.address();
            ensure(address != null, "address must not be null");
            inputs.add(address);
            address.collectInputsWithChildren(inputs);
            return inputs.stream();
          })
          .filter(x -> x instanceof LlvmReadRegFileNode)
          .map(x -> (LlvmReadRegFileNode) x)
          .toList();

      alternativePatterns.add(
          super.replaceRegisterWithFrameIndex(selector, machine, affectedNodes));
    }

    return alternativePatterns;
  }
}
