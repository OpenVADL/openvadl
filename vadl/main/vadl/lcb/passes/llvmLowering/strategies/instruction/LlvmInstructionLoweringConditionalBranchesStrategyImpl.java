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

import static vadl.gcb.passes.MachineInstructionLabel.BEQ;
import static vadl.gcb.passes.MachineInstructionLabel.BNEQ;
import static vadl.gcb.passes.MachineInstructionLabel.BSGEQ;
import static vadl.gcb.passes.MachineInstructionLabel.BSGTH;
import static vadl.gcb.passes.MachineInstructionLabel.BSLEQ;
import static vadl.gcb.passes.MachineInstructionLabel.BSLTH;
import static vadl.gcb.passes.MachineInstructionLabel.BUGEQ;
import static vadl.gcb.passes.MachineInstructionLabel.BUGTH;
import static vadl.gcb.passes.MachineInstructionLabel.BULEQ;
import static vadl.gcb.passes.MachineInstructionLabel.BULTH;
import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LoweringStrategyUtils;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
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
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Lowering conditional branch instructions into TableGen patterns.
 */
public class LlvmInstructionLoweringConditionalBranchesStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringConditionalBranchesStrategyImpl(ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(BEQ, BNEQ, BSGEQ, BSLEQ, BSLTH, BSGTH, BUGEQ, BULEQ, BULTH, BUGTH);
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks(
      PrintableInstruction instruction) {
    return replacementHooksWithFieldAccessWithBasicBlockReplacement(instruction);
  }

  @Override
  public Optional<LlvmLoweringRecord.Machine> lowerInstruction(
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      Instruction instruction,
      Graph uninlinedBehavior,
      Abi abi) {
    var visitor = replacementHooks(instruction);
    var copy = uninlinedBehavior.copy();

    for (var node : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, node);
    }

    copy.deinitializeNodes();
    return Optional.of(
        createIntermediateResult(labelledMachineInstructions, instruction, copy, abi));
  }

  private LlvmLoweringRecord.Machine createIntermediateResult(
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      Instruction instruction,
      Graph visitedGraph,
      Abi abi) {
    var info = lowerBaseInfo(visitedGraph);

    var writes = visitedGraph.getNodes(WriteResourceNode.class).toList();
    var patterns = generatePatterns(instruction, info.inputs(), writes);
    var alternatives =
        generatePatternVariations(instruction,
            supportedInstructions,
            visitedGraph,
            info.inputs(),
            info.outputs(),
            patterns,
            abi);

    var allPatterns = Stream.concat(patterns.stream(), alternatives.stream())
        .map(LoweringStrategyUtils::replaceBasicBlockByLabelImmediateInMachineInstruction)
        .toList();

    return new LlvmLoweringRecord.Machine(
        instruction,
        info,
        allPatterns,
        Collections.emptyList());
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
    var label = supportedInstructions.reverse().get(instruction);

    ArrayList<TableGenPattern> alternatives = new ArrayList<>();
    if (label != BEQ && label != BNEQ) {
      var swapped = generatePatternsForSwappedOperands(patterns);
      alternatives.addAll(swapped);
      alternatives.addAll(
          generateBrCondFromBrCc(Stream.concat(patterns.stream(), swapped.stream()).toList()));
    } else {
      alternatives.addAll(
          generateBrCondFromBrCc(patterns));
    }

    if (label == BNEQ) {
      alternatives.add(
          generateBrCondWithRegister(instruction, behavior));
    }

    return alternatives;
  }

  private TableGenPattern generateBrCondWithRegister(Instruction instruction, Graph behavior) {
    /*
     Generate the following pattern:

     def : Pat<(brcond X:$cond, bb:$imm12), (BNE X:$cond, X0, bb:$imm12)>;
     */

    var brcc = ensurePresent(
        behavior.getNodes(LlvmBrCcSD.class)
            .findFirst(), () -> Diagnostic.error("Cannot find a comparison in the behavior",
            instruction.location()));
    var rawRegister = (ReadRegTensorNode) brcc.first();
    var llvmRegisterNode =
        new LlvmReadRegFileNode(rawRegister.regTensor(),
            rawRegister.indices().getLast().copy(),
            rawRegister.type(), rawRegister.staticCounterAccess());
    var registerFile = rawRegister.regTensor();
    var zeroRegister = getZeroRegister(registerFile);

    var selector = new Graph("selector");
    selector.addWithInputs(new LlvmBrCondSD(llvmRegisterNode,
        brcc.immOffset().copy()));
    var machine = new Graph("machine");
    machine.addWithInputs(new LcbMachineInstructionNode(new NodeList<>(
        llvmRegisterNode.copy(),
        zeroRegister,
        brcc.immOffset().copy()
    ), instruction));

    return new TableGenSelectionWithOutputPattern(selector, machine);
  }

  private ConstantNode getZeroRegister(RegisterTensor registerFile) {
    var zeroConstraint =
        ensurePresent(
            Arrays.stream(registerFile.constraints()).filter(x -> x.value().intValue() == 0)
                .findFirst(),
            () -> Diagnostic.error("Cannot find zero constraint", registerFile.location()));
    var constant =
        new Constant.Str(
            registerFile.simpleName() + zeroConstraint.indices().getFirst().intValue());
    return new ConstantNode(constant);
  }

  private List<TableGenPattern> generatePatternsForSwappedOperands(List<TableGenPattern> patterns) {
    /*
      When we have a pattern with SEGE.

      def : Pat<(brcc SETGE, X:$rs1, X:$rs2, bb:$imm),
            (BGE X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;

      // Then swap the operands and replace the condCode with SETLE.

      def : Pat<(brcc SETLE, X:$rs2, X:$rs1, bb:$imm),
            (BGE X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;

      Of course, it might be the case that there is already such an instruction which covers that.
      But it is better to be sure.
     */
    ArrayList<TableGenPattern> alternatives = new ArrayList<>();

    for (var pattern : patterns) {
      if (pattern instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // We only need to consider the selector pattern.
        // There is no change required for the machine pattern.
        var copy = pattern.selector().copy();
        copy.getNodes(LlvmBrCcSD.class).forEach(llvmBrCcSD -> {
          var newCondCode = LlvmCondCode.inverse(llvmBrCcSD.condition());
          llvmBrCcSD.swapOperands(newCondCode);
        });
        alternatives.add(
            new TableGenSelectionWithOutputPattern(copy, outputPattern.machine().copy()));
      }
    }

    return alternatives;
  }

  private List<TableGenPattern> generateBrCondFromBrCc(
      List<TableGenPattern> patterns) {
    ArrayList<TableGenPattern> alternatives = new ArrayList<>();

    // Generate brcond patterns from brcc
    /*
    def : Pat<(brcc SETEQ, X:$rs1, X:$rs2, bb:$imm),
          (BEQ X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;

    to

    def : Pat<(brcond (i32 (seteq X:$rs1, X:$rs2)), bb:$imm12),
        (BEQ X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;
     */

    for (var pattern : patterns.stream()
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x).toList()) {
      var selector = pattern.selector().copy();
      var machine = pattern.machine().copy();

      // Replace BrCc with BrCond
      var hasChanged = false;
      for (var node : selector.getNodes(LlvmBrCcSD.class).toList()) {
        // For `brcc` we have Setcc code, so need to see if we have a suitable
        // instruction for that.
        var builtin = LlvmCondCode.from(node.condition());
        var builtinCall =
            new LlvmSetCondSD(builtin, new NodeList<>(node.first(), node.second()),
                node.first().type());

        // We also extend the result of the condition to i32 or i64.
        var typeCast = new LlvmTypeCastSD(builtinCall, node.immOffset().type());
        var brCond = new LlvmBrCondSD(typeCast, node.immOffset());
        node.replaceAndDelete(brCond);
        hasChanged = true;
      }

      // If nothing had changed, then it makes no sense to add it.
      if (hasChanged) {
        alternatives.add(new TableGenSelectionWithOutputPattern(selector, machine));
      }
    }

    return alternatives;
  }
}
