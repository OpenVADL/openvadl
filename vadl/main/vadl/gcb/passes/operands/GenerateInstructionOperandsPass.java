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

package vadl.gcb.passes.operands;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.gcb.passes.operands.model.TableGenConstantOperand;
import vadl.gcb.passes.operands.model.TableGenInstructionBareSymbolOperand;
import vadl.gcb.passes.operands.model.TableGenInstructionIndexedRegisterFileOperand;
import vadl.gcb.passes.operands.model.TableGenInstructionOperand;
import vadl.gcb.passes.operands.model.TableGenInstructionRegisterFileOperand;
import vadl.gcb.passes.pseudo.PseudoFuncParamNode;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Generates the input and output operands for instructions.
 */
public class GenerateInstructionOperandsPass extends Pass {

  /**
   * Constructor.
   */
  public GenerateInstructionOperandsPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateInstructionOperandsPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    machineInstructions(viam);
    pseudoInstructions(viam);
    compilerInstructions(viam);

    return null;
  }

  private void machineInstructions(Specification viam) {
    for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
      var outputOperands = getTableGenOutputOperands(instruction.behavior());
      var inputOperands = getTableGenInputOperands(outputOperands, instruction.behavior());

      var ctx = new InstructionOperandsCtx(inputOperands, outputOperands);
      instruction.attachExtension(ctx);
    }
  }

  private void pseudoInstructions(Specification viam) {
    for (var instruction : viam.isa().orElseThrow().ownPseudoInstructions()) {
      var outputOperands = getTableGenOutputOperands(instruction.behavior());
      var inputOperands = getTableGenInputOperands(outputOperands, instruction.behavior());

      var ctx = new PseudoInstructionOperandsCtx(inputOperands, outputOperands);
      instruction.attachExtension(ctx);
    }
  }

  private void compilerInstructions(Specification viam) {
    var abi = viam.abi().orElseThrow();
    for (var instruction : Stream.concat(abi.constantSequences().stream(),
        abi.registerAdjustmentSequences().stream()).toList()) {
      var outputOperands = getTableGenOutputOperands(instruction.behavior());
      var inputOperands = getTableGenInputOperands(outputOperands, instruction.behavior());

      var ctx = new PseudoInstructionOperandsCtx(inputOperands, outputOperands);
      instruction.attachExtension(ctx);
    }
  }

  private List<TableGenInstructionOperand> getTableGenOutputOperands(Graph behavior) {
    var operands = extractWrites(behavior);
    return operands
        .stream()
        .filter(operand -> {
          // Why?
          // Because LLVM cannot handle static registers in input or output operands.
          // They belong to defs and uses instead.
          return !operand.hasConstantAddress();
        })
        .map(this::map)
        .toList();
  }

  /**
   * Most instruction's behaviors have inputs. Those are the results which the instruction requires.
   */
  private static List<Node> getInputOperands(Graph graph) {
    // First, the registers
    var x = graph.getNodes(ReadRegTensorNode.class).filter(k -> k.regTensor().isRegisterFile());
    // Then, immediates
    var y = graph.getNodes(FieldAccessRefNode.class);
    // Then, the rest
    var z = graph.getNodes(FuncCallNode.class).flatMap(
        funcCallNode -> funcCallNode.function().behavior().getNodes(FuncParamNode.class));

    // We need this edge case for compiler and pseudo instructions.
    // However, we need to filter for `WriteResourceNode` and `ReadResourceNode`, so
    // the operand is not added twice.
    var u = graph.getNodes(PseudoFuncParamNode.class)
        .filter(k -> k.usages()
            .noneMatch(v -> v instanceof WriteResourceNode || v instanceof ReadResourceNode));

    return Stream.concat(Stream.concat(Stream.concat(x, y), z), u)
        .map(k -> (Node) k).toList();
  }

  /**
   * Extracts the input operands from the {@link Graph}. But it will skip nodes which are
   * already a {@link Node} in the {@code outputOperands}. Because if you have a
   * {@link PseudoInstruction} like {@code ADDI rd, rd, 1} then is the output and one input
   * the same which tablegen will not accept.
   */
  private List<TableGenInstructionOperand> getTableGenInputOperands(
      List<TableGenInstructionOperand> outputOperands,
      Graph graph) {

    var inputOperands = getInputOperands(graph)
        .stream()
        .filter(node -> {
          // Why?
          // Because LLVM cannot handle static registers in input or output operands.
          // They belong to defs and uses instead.
          if (node instanceof ReadRegTensorNode readRegTensorNode
              && readRegTensorNode.regTensor().isRegisterFile()) {
            return !readRegTensorNode.hasConstantAddress();
          }
          return true;
        })
        .map(this::map)
        .toList();

    return filterOutputs(outputOperands, inputOperands.stream())
        .toList();
  }

  private TableGenInstructionOperand map(Node operand) {
    if (operand instanceof ReadRegTensorNode node && node.regTensor().isRegisterFile()) {
      return mapFrom(node);
    } else if (operand instanceof WriteRegTensorNode node && node.regTensor().isRegisterFile()) {
      return mapFrom(node);
    } else if (operand instanceof FuncParamNode node) {
      return mapFrom(node);
    } else if (operand instanceof FieldAccessRefNode node) {
      return mapFrom(node);
    } else {
      throw Diagnostic.error(
          "Cannot construct a tablegen instruction operand from the type.",
          operand.location()).build();
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private TableGenInstructionOperand mapFrom(FieldAccessRefNode node) {
    return new GcbInstructionImmediateOperand(node);
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private TableGenInstructionOperand mapFrom(FuncParamNode node) {
    return new TableGenInstructionBareSymbolOperand(node,
        node.parameter().simpleName());
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private TableGenInstructionOperand mapFrom(
      ReadRegTensorNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new TableGenInstructionRegisterFileOperand(node, field);
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new TableGenInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else if (node.address() instanceof ConstantNode constantNode) {
      // The register file has a constant as address.
      // This is ok as long as the value of the register file at the address is also constant.
      // For example, the X0 register in RISC-V which always has a constant value.
      var constraints = Arrays.stream(node.regTensor().constraints()).toList();
      var constraintValue = constraints.stream()
          .filter(
              x -> x.indices().getFirst().intValue() == constantNode.constant().asVal().intValue())
          .findFirst();
      var constRegisterValue = ensurePresent(constraintValue,
          () -> Diagnostic.error("Register file with constant index has no constant value.",
                  constantNode.location())
              .help("Consider adding a constraint to register file for the given index."));
      // Update the type of the constant because it needs to be upcasted.
      // Heuristically, we take the type of the index because indices were also upcasted.
      var constantValue = constRegisterValue.value();
      constantValue.setType(constantNode.type());
      return new TableGenConstantOperand(constantNode, constantValue);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  private TableGenInstructionOperand mapFrom(WriteRegTensorNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new TableGenInstructionRegisterFileOperand(node, field);
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new TableGenInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  /**
   * It is not allowed to have a {@link TableGenInstructionOperand} in the input list
   * when it is already in the output list. That's why we compute the {@code outputOperands}
   * first and then filter out the {@code stream} for elements which already present in
   * {@code outputOperands}.
   */
  protected static Stream<TableGenInstructionOperand> filterOutputs(
      List<TableGenInstructionOperand> outputOperands,
      Stream<TableGenInstructionOperand> stream) {
    /*
    pseudo instruction LA( rd: Index, symbol: Bits<32> ) =
    {
      LUI { rd = rd, imm = hi( symbol ) }
      ADDI { rd = rd, rs1 = rd, imm = lo( symbol ) }
    }

    Here ADDI has a destination `rd` and an input `rs1` which is the same register as the
    destination. For these cases, we do not want the operand in the inputs.
     */

    var visited =
        outputOperands.stream()
            .filter(x -> x instanceof TableGenInstructionRegisterFileOperand
                || x instanceof TableGenInstructionIndexedRegisterFileOperand)
            .collect(Collectors.toSet());

    return stream
        .filter(
            node -> !visited.contains(node));
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private List<WriteRegTensorNode> extractWrites(Graph graph) {
    return graph.getNodes(WriteRegTensorNode.class)
        .filter(e -> e.regTensor().isRegisterFile())
        .toList();
  }
}
