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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.OutputInstructionName;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAddSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrindSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTargetCallSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstExpansionPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionRegisterFileOperand;
import vadl.types.Type;
import vadl.viam.Abi;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Generates the {@link LlvmLoweringRecord} for {@link MachineInstructionLabel#JALR}
 * instruction.
 */
public class LlvmInstructionLoweringIndirectJumpStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringIndirectJumpStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(MachineInstructionLabel.JALR);
  }

  @Override
  protected LlvmLoweringPass.Flags getFlags(Graph graph) {
    var flags = super.getFlags(graph);

    return LlvmLoweringPass.Flags.withIsRematerialisable(
        LlvmLoweringPass.Flags.withIsAsCheapAsMove(flags));
  }

  @Override
  public LlvmLoweringPass.BaseInstructionInfo lowerBaseInfo(Graph behavior) {
    var baseInfo = super.lowerBaseInfo(behavior);

    // Clear the flags
    baseInfo = baseInfo.withFlags(LlvmLoweringPass.Flags.empty());

    return baseInfo;
  }

  @Override
  public Optional<LlvmLoweringRecord.Machine> lowerInstruction(
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      Instruction instruction,
      Graph unmodifiedBehavior,
      Abi abi) {
    var copy = unmodifiedBehavior.copy();
    var visitor = replacementHooksWithDefaultFieldAccessReplacement(instruction);

    for (var node : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, node);
    }

    var info = lowerBaseInfo(copy);

    // Clear the flags for this strategy
    info = info.withFlags(LlvmLoweringPass.Flags.empty());

    var patterns = generatePatternVariations(instruction,
        labelledMachineInstructions,
        copy,
        info.inputs(),
        info.outputs(),
        Collections.emptyList(),
        abi);

    return Optional.of(new LlvmLoweringRecord.Machine(
        instruction,
        info,
        patterns,
        Collections.emptyList()));
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks(
      PrintableInstruction printableInstruction) {
    return replacementHooksWithDefaultFieldAccessReplacement(printableInstruction);
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
    var result = new ArrayList<TableGenPattern>();
    inputOperands.stream().filter(x -> x instanceof TableGenInstructionRegisterFileOperand)
        .findFirst()
        .ifPresent((uncastInputRegister) -> {
          result.add(generateIndirectCall(supportedInstructions, abi,
              (TableGenInstructionRegisterFileOperand) uncastInputRegister));
          result.add(generateBranchIndirect(instruction, supportedInstructions,
              (TableGenInstructionRegisterFileOperand) uncastInputRegister));
          result.add(generateBranchIndirectWithZero(
              (TableGenInstructionRegisterFileOperand) uncastInputRegister));
          result.add(generateBranchIndirectWithAdd(instruction, supportedInstructions,
              (TableGenInstructionRegisterFileOperand) uncastInputRegister));
        });

    return result;
  }


  private static @Nonnull TableGenPseudoInstExpansionPattern generateIndirectCall(
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      Abi abi,
      TableGenInstructionRegisterFileOperand inputRegister) {
    var selector = new Graph("selector");
    var ref = (ReadRegTensorNode) inputRegister.origin().copy();
    var address = (FieldRefNode) ref.address().copy();
    selector.addWithInputs(new LlvmTargetCallSD(new NodeList<>(new LlvmReadRegFileNode(
        inputRegister.registerFile(), address, inputRegister.formatField().type(),
        ref.staticCounterAccess()
    )),
        Type.dummy()));

    var database = new Database(supportedInstructions);
    var jalr =
        database.run(
                new Query.Builder().machineInstructionLabel(MachineInstructionLabel.JALR).build())
            .firstMachineInstruction();
    var machine = new Graph("machine");
    var constant = new Constant.Str("0");
    machine.addWithInputs(new LcbMachineInstructionNode(
        new NodeList<>(new ConstantNode(new Constant.Str(abi.returnAddress().render())), ref,
            new ConstantNode(constant)), jalr));
    return new TableGenPseudoInstExpansionPattern("PseudoCALLIndirect",
        selector,
        machine,
        true,
        false,
        false,
        false,
        false,
        List.of(
            new TableGenInstructionRegisterFileOperand(ref, address)
        ), Collections.emptyList(),
        List.of(
            new RegisterRef(abi.returnAddress().registerFile(),
                Constant.Value.of(abi.returnAddress().addr(),
                    abi.returnAddress().registerFile().resultType())))
    );
  }

  private static @Nonnull TableGenPseudoInstExpansionPattern generateBranchIndirect(
      Instruction instruction,
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      TableGenInstructionRegisterFileOperand inputRegister) {
    /*
    let isCall = 1, isBranch = 1, isIndirectBranch = 1, isTerminator = 1,
      isBarrier = 1
      in
          def PseudoBRIND : Pseudo<(outs ), (ins X:$rs1, RV32IM_Itype_immAsInt32:$imm), []>,
                       PseudoInstExpansion<(JALR X0, X:$rs1, RV32IM_Itype_immAsInt32:$imm)>;
     */

    var selector = new Graph("selector");
    var ref = (ReadRegTensorNode) inputRegister.origin().copy();
    var address = (FieldRefNode) ref.address().copy();

    var database = new Database(supportedInstructions);
    var jalr =
        database.run(
                new Query.Builder().machineInstructionLabel(MachineInstructionLabel.JALR).build())
            .firstMachineInstruction();
    var immediate = ensurePresent(jalr.behavior().getNodes(FieldAccessRefNode.class).findFirst(),
        () -> Diagnostic.error("Cannot find an immediate.", jalr.location()));
    var llvmType = ensurePresent(ValueType.from(immediate.fieldAccess().type()),
        () -> Diagnostic.error("Cannot construct llvm type from field access",
            immediate.location()));
    var fieldRef =
        new LlvmFieldAccessRefNode(instruction, immediate.fieldAccess(), immediate.type(), llvmType,
            LlvmFieldAccessRefNode.Usage.Immediate);
    var machine = new Graph("machine");
    machine.addWithInputs(new LcbMachineInstructionNode(
        new NodeList<>(
            new ConstantNode(new Constant.Str(zeroRegister(inputRegister.registerFile()))),
            ref,
            fieldRef), jalr));
    return new TableGenPseudoInstExpansionPattern("PseudoBRIND",
        selector,
        machine,
        true,
        true,
        true,
        true,
        true,
        List.of(
            new TableGenInstructionRegisterFileOperand(ref, address),
            new TableGenInstructionImmediateOperand(fieldRef)
        ), Collections.emptyList(),
        Collections.emptyList());
  }

  @Nonnull
  private TableGenPattern generateBranchIndirectWithAdd(
      Instruction instruction,
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      TableGenInstructionRegisterFileOperand inputRegister) {
    var database = new Database(supportedInstructions);
    var jalr =
        database.run(
                new Query.Builder().machineInstructionLabel(MachineInstructionLabel.JALR).build())
            .firstMachineInstruction();
    var immediate = ensurePresent(
        jalr.behavior().getNodes(FieldAccessRefNode.class).findFirst(),
        () -> Diagnostic.error("Cannot find immediate.", jalr.location()));

    var selector = new Graph("selector");
    var ref = (ReadRegTensorNode) inputRegister.origin().copy();
    var address = (FieldRefNode) ref.address().copy();
    var llvmRegister = new LlvmReadRegFileNode(
        inputRegister.registerFile(), address, inputRegister.formatField().type(),
        ref.staticCounterAccess()
    );

    var llvmType = ensurePresent(ValueType.from(immediate.fieldAccess().type()),
        () -> Diagnostic.error("Cannot construct llvm type from field access",
            immediate.location()));
    var fieldRef =
        new LlvmFieldAccessRefNode(instruction, immediate.fieldAccess(), immediate.type(), llvmType,
            LlvmFieldAccessRefNode.Usage.Immediate);
    selector.addWithInputs(new LlvmBrindSD(new NodeList<>(
        new LlvmAddSD(new NodeList<>(llvmRegister, fieldRef), Type.dummy())),
        Type.dummy()));

    var machine = new Graph("machine");
    machine.addWithInputs(new LcbMachineInstructionNode(
        new NodeList<>(llvmRegister.copy(),
            fieldRef.copy()),
        new OutputInstructionName("PseudoBRIND")));

    return new TableGenSelectionWithOutputPattern(selector, machine);
  }

  private TableGenPattern generateBranchIndirectWithZero(
      TableGenInstructionRegisterFileOperand inputRegister) {
    var selector = new Graph("selector");
    var ref = (ReadRegTensorNode) inputRegister.origin().copy();
    var address = (FieldRefNode) ref.address().copy();
    var llvmRegister = new LlvmReadRegFileNode(
        inputRegister.registerFile(), address, inputRegister.formatField().type(),
        ref.staticCounterAccess()
    );
    var constant = new Constant.Str("0");
    selector.addWithInputs(new LlvmBrindSD(new NodeList<>(
        llvmRegister),
        Type.dummy()));

    var machine = new Graph("machine");
    machine.addWithInputs(new LcbMachineInstructionNode(
        new NodeList<>(llvmRegister.copy(),
            new ConstantNode(constant)),
        new OutputInstructionName("PseudoBRIND")));

    return new TableGenSelectionWithOutputPattern(selector, machine);
  }

  private static String zeroRegister(RegisterTensor registerFile) {
    var constraint =
        ensurePresent(
            Arrays.stream(registerFile.constraints()).filter(x -> x.value().intValue() == 0)
                .findFirst(),
            () -> Diagnostic.error("There must a constraint for the zero register.",
                registerFile.location())
        );

    return registerFile.simpleName() + constraint.indices().getFirst().intValue();
  }
}
