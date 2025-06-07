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

package vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals;

import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.types.BuiltInTable;
import vadl.viam.Abi;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Lowering of less-than-unsigned conditionals into TableGen.
 */
public class LlvmInstructionLoweringLessThanUnsignedConditionalsStrategyImpl
    extends LlvmInstructionLoweringStrategy {

  private final Set<MachineInstructionLabel> supported =
      Set.of(MachineInstructionLabel.LTU);

  public LlvmInstructionLoweringLessThanUnsignedConditionalsStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return this.supported;
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

    var ltu = getFirst(instruction, supportedInstructions.labels(), MachineInstructionLabel.LTU);
    var xor = getFirst(instruction, supportedInstructions.labels(), MachineInstructionLabel.XOR);
    var xori = getFirst(instruction, supportedInstructions.labels(), MachineInstructionLabel.XORI);

    neq(ltu, xor, patterns, result);
    uge(xori, patterns, result);
    ugt(patterns, result);
    uleq(xori, patterns, result);

    return result;
  }

  /**
   * Goes over the patterns and tries to find a register-register. It sets the condition to
   * {@link LlvmCondCode#SETUGT}. It then swaps the operand.
   */
  private void ugt(List<TableGenPattern> patterns,
                   List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETULT),
                  (SLTU X:$rs1, X:$rs2)>;

                  to

              def : Pat< (setcc X:$rs1, X:$rs2, SETUGT ),
                  (SLTU X:$rs2, X:$rs1)>;
    */

    for (var pattern : patterns) {
      var copy = pattern.copy();

      if (copy instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // Change condition code
        var setcc = ensurePresent(
            outputPattern.selector().getNodes(LlvmSetccSD.class).toList().stream()
                .findFirst(),
            () -> Diagnostic.error("No setcc node was found", pattern.selector()
                .sourceLocation()));
        // Only RR and not RI should be replaced here.
        if (setcc.arguments().size() > 2
            && setcc.arguments().get(0) instanceof LlvmReadRegFileNode
            && setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.SGTH);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(LlvmCondCode.SETUGT.name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        // Change machine instruction to immediate
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(node -> {
              // Swap the operands
              Collections.reverse(node.arguments());
            });

        result.add(outputPattern);
      }
    }
  }

  private Instruction getFirst(
      Instruction instruction,
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      MachineInstructionLabel label) {
    return ensurePresent(supportedInstructions.getOrDefault(label, Collections.emptyList())
            .stream().findFirst(),
        () -> Diagnostic.error(String.format("No instruction with label '%s' detected.", label),
            instruction.location()));
  }

  /**
   * Goes over the patterns and tries to find a register-register. It sets the condition to
   * {@link LlvmCondCode#SETULE}. It then wraps the machine instruction with {@code xori}.
   */
  private void uleq(Instruction xori,
                    List<TableGenPattern> patterns,
                    List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETULT),
                  (SLTU X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, X:$rs2, SETULE ),
                   ( XORI ( SLTU X:$rs1, X:$rs2 ), 1 ) >;
               */
    for (var pattern : patterns) {
      var copy = pattern.copy();

      if (copy instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // Change condition code
        var setcc = ensurePresent(
            outputPattern.selector().getNodes(LlvmSetccSD.class).toList().stream()
                .findFirst(),
            () -> Diagnostic.error("No setcc node was found", pattern.selector()
                .sourceLocation()));
        // Only RR and not RI should be replaced here.
        if (setcc.arguments().size() > 2
            && setcc.arguments().get(0) instanceof LlvmReadRegFileNode
            && setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.ULEQ);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(setcc.llvmCondCode().name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        var machineInstruction =
            outputPattern.machine().getNodes(LcbMachineInstructionNode.class).findFirst().get();
        Collections.reverse(machineInstruction.arguments());
        var newMachineInstruction = new LcbMachineInstructionNode(
            new NodeList<>(machineInstruction, new ConstantNode(new Constant.Str("1"))), xori);
        outputPattern.machine().addWithInputs(newMachineInstruction);

        result.add(outputPattern);
      }
    }
  }

  /**
   * Goes over the patterns and tries to find a register-register. It sets the condition to
   * {@link LlvmCondCode#SETUGE}. It then wraps the machine instruction with {@code xori}.
   */
  private void uge(Instruction xori,
                   List<TableGenPattern> patterns,
                   List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETULT),
                  (SLTU X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, X:$rs2, SETUGE ),
                   ( XORI ( SLTU X:$rs1, X:$rs2 ), 1 ) >;
               */
    for (var pattern : patterns) {
      var copy = pattern.copy();

      if (copy instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // Change condition code
        var setcc = ensurePresent(
            outputPattern.selector().getNodes(LlvmSetccSD.class).toList().stream()
                .findFirst(),
            () -> Diagnostic.error("No setcc node was found", pattern.selector()
                .sourceLocation()));
        // Only RR and not RI should be replaced here.
        if (setcc.arguments().size() > 2
            && setcc.arguments().get(0) instanceof LlvmReadRegFileNode
            && setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.UGEQ);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(setcc.llvmCondCode().name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        var machineInstruction =
            outputPattern.machine().getNodes(LcbMachineInstructionNode.class).findFirst().get();
        var newMachineInstruction = new LcbMachineInstructionNode(
            new NodeList<>(machineInstruction, new ConstantNode(new Constant.Str("1"))), xori);
        outputPattern.machine().addWithInputs(newMachineInstruction);

        result.add(outputPattern);
      }
    }
  }

  /**
   * Goes over the patterns and tries to find a register-register. It sets the condition to
   * {@link LlvmCondCode#SETNE}. It then wraps the machine instruction with the {@code sltu}.
   * The inner instruction will be replaced by {@code xor}.
   */
  private void neq(Instruction sltu,
                   Instruction xor,
                   List<TableGenPattern> patterns,
                   List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                  (SLT X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, X:$rs2, SETNE ),
                  ( SLTU X0, ( XOR X:$rs1, X:$rs2 ) ) >;
               */
    for (var pattern : patterns) {
      var copy = pattern.copy();

      if (copy instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // Change condition code
        var setcc = ensurePresent(
            outputPattern.selector().getNodes(LlvmSetccSD.class).toList().stream()
                .findFirst(),
            () -> Diagnostic.error("No setcc node was found", pattern.selector()
                .sourceLocation()));
        // Only RR and not RI should be replaced here.
        if (setcc.arguments().size() > 2
            && setcc.arguments().get(0) instanceof LlvmReadRegFileNode
            && setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.NEQ);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(setcc.llvmCondCode().name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        // Change machine instruction to immediate
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(node -> {
              node.setOutputInstruction(sltu);

              var registerFile =
                  ensurePresent(
                      xor.behavior().getNodes(ReadRegTensorNode.class)
                          .filter(x -> x.regTensor().isRegisterFile())
                          .map(ReadRegTensorNode::regTensor)
                          .findFirst(),
                      () -> Diagnostic.error("Cannot find a register", xor.location()));

              var zeroConstraint =
                  ensurePresent(
                      Arrays.stream(registerFile.constraints())
                          .filter(x -> x.value().intValue() == 0)
                          .findFirst(),
                      () -> Diagnostic.error("Cannot find zero register for register file",
                          registerFile.location()));
              // Cannot construct a `ReadReg` because this register does not really exist.
              // (for the VIAM spec)
              var zeroRegister = new ConstantNode(
                  new Constant.Str(
                      registerFile.simpleName() + zeroConstraint.indices().getFirst().intValue()));

              var newArgs = new LcbMachineInstructionNode(node.arguments(), xor);
              node.setArgs(
                  new NodeList<>(zeroRegister, newArgs));
            });

        result.add(outputPattern);
      }
    }
  }
}
