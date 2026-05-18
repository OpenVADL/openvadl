// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.RegisterRef;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.OutputInstructionName;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrindSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadResourceFactory;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTargetCallSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstExpansionPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.types.Type;
import vadl.viam.Abi;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Generates the {@link LlvmLoweringRecord} for {@link MachineInstructionLabel#BLR}
 * instructions: indirect branch-and-link without an immediate operand (aarch64-style).
 * The structural difference from {@link MachineInstructionLabel#JALR} is the absence
 * of an immediate, so the emitted machine pattern has a single register operand.
 */
public class LlvmInstructionLoweringIndirectJumpAndLinkNoImmediateStrategyImpl
    extends LlvmInstructionLoweringIndirectJumpAndLinkStrategyImpl {
  public LlvmInstructionLoweringIndirectJumpAndLinkNoImmediateStrategyImpl(
      ValueType architectureType, ValueType smallestRegisterClassType) {
    super(architectureType, smallestRegisterClassType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(MachineInstructionLabel.BLR);
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
    var result = new ArrayList<TableGenPattern>();
    inputOperands.stream().filter(x -> x instanceof GcbInstructionRegisterFileOperand)
        .findFirst()
        .ifPresent((uncastInputRegister) -> {
          var inputRegister = (GcbInstructionRegisterFileOperand) uncastInputRegister;
          result.add(generateIndirectCall(supportedInstructions, abi, inputRegister));
          result.add(generateBranchIndirectExpansion(supportedInstructions, inputRegister));
          result.add(generateBranchIndirectSelection(inputRegister));
        });

    return result;
  }

  private static @Nonnull TableGenPseudoInstExpansionPattern generateIndirectCall(
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      Abi abi,
      GcbInstructionRegisterFileOperand inputRegister) {
    var selector = new Graph("selector");
    var machine = new Graph("machine");

    var ref = (ReadsRegisterTensor) inputRegister.origin().copy();
    var address = (FieldRefNode) ref.indices().getFirst().copy();
    var counter = ref instanceof ReadRegTensorNode rn 
        ? rn.staticCounterAccess()
        : null;
    var llvmReadRegFileNode = new LlvmReadResourceFactory().create(ref.registerResource(), address,
        ref.type(), counter);

    var database = new Database(supportedInstructions);
    var blr =
        database.run(
                new Query.Builder().machineInstructionLabel(MachineInstructionLabel.BLR).build())
            .firstMachineInstruction();

    selector.addWithInputs(new LlvmTargetCallSD(
        new NodeList<>(llvmReadRegFileNode),
        Type.dummy()));
    machine.addWithInputs(new LcbMachineInstructionNode(
        new NodeList<>(llvmReadRegFileNode.copy()), blr));
    return new TableGenPseudoInstExpansionPattern("PseudoCALLIndirect",
        selector,
        machine,
        true,
        false,
        false,
        false,
        false,
        List.of(
          llvmReadRegFileNode instanceof ReadRegTensorNode rn
            ? new GcbInstructionRegisterFileOperand(rn, address.formatField())
            : new GcbInstructionRegisterFileOperand((ReadArtificialResNode) llvmReadRegFileNode,
                address.formatField())),
        Collections.emptyList(),
        List.of(
            new RegisterRef(abi.returnAddress().registerFile(),
                Constant.Value.of(abi.returnAddress().addr(),
                    abi.returnAddress().registerFile().resultType())))
    );
  }

  private static @Nonnull TableGenPseudoInstExpansionPattern generateBranchIndirectExpansion(
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      GcbInstructionRegisterFileOperand inputRegister) {
    var machine = new Graph("machine");
    var selector = new Graph("selector");
    var ref = (ReadsRegisterTensor) inputRegister.origin();
    var address = (FieldRefNode) ref.indices().getFirst().copy();
    var counter = ref instanceof ReadRegTensorNode rn
        ? rn.staticCounterAccess()
        : null;
    var llvmReadRegFileNode = new LlvmReadResourceFactory().create(ref.registerResource(), address,
        ref.type(), counter);

    var database = new Database(supportedInstructions);
    var blr =
        database.run(
                new Query.Builder().machineInstructionLabel(MachineInstructionLabel.BLR).build())
            .firstMachineInstruction();
    machine.addWithInputs(new LcbMachineInstructionNode(
        new NodeList<>(llvmReadRegFileNode.copy()), 
        blr));
    return new TableGenPseudoInstExpansionPattern("PseudoBRIND",
        selector,
        machine,
        false,
        true,
        true,
        true,
        true,
        List.of(
          llvmReadRegFileNode instanceof ReadRegTensorNode rn
            ? new GcbInstructionRegisterFileOperand(rn, address.formatField())
            : new GcbInstructionRegisterFileOperand((ReadArtificialResNode) llvmReadRegFileNode,
                address.formatField())),
        Collections.emptyList(),
        Collections.emptyList());
  }

  private TableGenPattern generateBranchIndirectSelection(
      GcbInstructionRegisterFileOperand inputRegister) {
    var selector = new Graph("selector");
    var ref = (ReadsRegisterTensor) inputRegister.origin();
    var address = (FieldRefNode) ref.indices().getFirst().copy();
    var factory = new LlvmReadResourceFactory();
    var counter = ref instanceof ReadRegTensorNode rn
        ? rn.staticCounterAccess()
        : null;
    var llvmReadRegFileNode = factory.create(
        ref.registerResource(), address, inputRegister.formatField().type(),
        counter);
    selector.addWithInputs(new LlvmBrindSD(new NodeList<>(llvmReadRegFileNode), Type.dummy()));

    var machine = new Graph("machine");
    machine.addWithInputs(new LcbMachineInstructionNode(
        new NodeList<>(llvmReadRegFileNode.copy()),
        new OutputInstructionName("PseudoBRIND")));

    return new TableGenSelectionWithOutputPattern(selector, machine);
  }
}
