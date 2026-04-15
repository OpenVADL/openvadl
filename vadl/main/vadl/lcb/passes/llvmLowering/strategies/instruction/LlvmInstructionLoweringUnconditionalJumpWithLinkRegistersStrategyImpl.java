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

import static vadl.gcb.passes.MachineInstructionLabel.JAL;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import vadl.gcb.passes.DetermineRegisterUsesAndDefsPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionConstraint;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.types.DataType;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Lowering unconditional jump instructions into TableGen patterns.
 */
public class LlvmInstructionLoweringUnconditionalJumpWithLinkRegistersStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringUnconditionalJumpWithLinkRegistersStrategyImpl(
      ValueType architectureType, ValueType smallestRegisterWidth) {
    super(architectureType, smallestRegisterWidth);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(JAL);
  }

  @Override
  public Optional<LlvmLoweringRecord.Machine> lowerInstruction(
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      Instruction instruction,
      Graph uninlinedBehavior,
      Abi abi,
      DetermineRegisterUsesAndDefsPass.Info registerDefsUses,
      boolean generatePatterns) {
    var copy = uninlinedBehavior.copy();

    var constraints = generateConstraints(copy);
    for (var node : copy.getNodes(SideEffectNode.class).toList()) {
      replaceNode(instruction, node);
    }

    return Optional.of(
        createIntermediateResult(instruction, copy, registerDefsUses, constraints));
  }

  private LlvmLoweringRecord.Machine createIntermediateResult(
      Instruction instruction,
      Graph uninlinedGraph,
      DetermineRegisterUsesAndDefsPass.Info registerDefsUses,
      List<TableGenInstructionConstraint> constraints) {

    var info = lowerBaseInfo(instruction, uninlinedGraph, registerDefsUses);
    var unchangedFlags = getFlags(uninlinedGraph);
    var flags = LlvmLoweringPass.Flags.withNoTerminator(
        LlvmLoweringPass.Flags.withNoBranch(unchangedFlags));

    return new LlvmLoweringRecord.Machine(
        instruction,
        info.withFlags(flags),
        Collections.emptyList(),
        Collections.emptyList(),
        constraints);
  }

  @Override
  protected List<TableGenPattern> generatePatterns(Instruction instruction,
                                                   Graph behavior,
                                                   List<GcbInstructionOperand> inputOperands,
                                                   List<WriteResourceNode> sideEffectNodes) {
    throw new RuntimeException("Must not be called. Use the other method");
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
    return Collections.emptyList();
  }
}
