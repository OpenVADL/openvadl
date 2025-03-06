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

import static vadl.gcb.passes.MachineInstructionLabel.SDIV;
import static vadl.gcb.passes.MachineInstructionLabel.SMOD;
import static vadl.gcb.passes.MachineInstructionLabel.UDIV;
import static vadl.gcb.passes.MachineInstructionLabel.UMOD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.types.BuiltInTable;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Lowers division into {@link TableGenInstruction}.
 */
public class LlvmInstructionLoweringDivisionAndRemainderStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  private final Set<BuiltInTable.BuiltIn> supportedBuiltins =
      Set.of(BuiltInTable.SDIV, BuiltInTable.SDIVS, BuiltInTable.UDIV, BuiltInTable.UDIVS,
          BuiltInTable.SMOD,
          BuiltInTable.UMOD, BuiltInTable.SMODS, BuiltInTable.UMODS);
  private final Set<MachineInstructionLabel> supported = Set.of(SDIV, UDIV, SMOD, UMOD);

  public LlvmInstructionLoweringDivisionAndRemainderStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return supported;
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks() {
    return replacementHooksWithDefaultFieldAccessReplacement();
  }

  @Override
  public Optional<LlvmLoweringRecord> lowerInstruction(
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      Instruction instruction,
      Graph unmodifiedBehavior,
      Abi abi) {
    var visitor = replacementHooksWithDefaultFieldAccessReplacement();
    var copy = unmodifiedBehavior.copy();

    for (var endNode : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, endNode);
    }

    var info = lowerBaseInfo(copy);
    var inputOperands = getInputs(info.outputs(), copy);

    copy.deinitializeNodes();

    var patterns = generatePatterns(instruction,
        inputOperands,
        copy.getNodes(WriteResourceNode.class).toList());
    return Optional.of(new LlvmLoweringRecord(
        new LlvmLoweringPass.BaseInstructionInfo(inputOperands,
            info.outputs(),
            info.flags(),
            info.uses(),
            info.defs()),
        patterns
    ));
  }

  private List<TableGenInstructionOperand> getInputs(
      List<TableGenInstructionOperand> outputOperands, Graph graph) {
    var inputOperands = graph.getNodes(BuiltInCall.class)
        .filter(x -> supportedBuiltins.contains(x.builtIn()))
        .flatMap(x -> x.arguments().stream())
        .filter(x -> x instanceof ReadRegFileNode)
        .map(LlvmInstructionLoweringStrategy::generateTableGenInputOutput);

    return filterOutputs(outputOperands, inputOperands).toList();
  }

  @Override
  protected List<TableGenPattern> generatePatterns(
      Instruction instruction,
      List<TableGenInstructionOperand> inputOperands,
      List<WriteResourceNode> sideEffectNodes) {
    ArrayList<TableGenPattern> patterns = new ArrayList<>();

    // The side effect is not relevant. We are looking for
    // the builtin in the call.
    var children = new ArrayList<Node>();
    sideEffectNodes.forEach(x -> x.collectInputsWithChildren(children));

    children.stream().filter(
            x -> x instanceof BuiltInCall bs
                && supportedBuiltins.contains(bs.builtIn()))
        .forEach(node -> {
          var builtInCall = (BuiltInCall) node;
          var patternSelector = getPatternSelector(builtInCall);
          var machineInstruction = getOutputPattern(instruction, inputOperands);
          patterns.add(
              new TableGenSelectionWithOutputPattern(patternSelector, machineInstruction));
        });

    return patterns;
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
    return Collections.emptyList();
  }
}
