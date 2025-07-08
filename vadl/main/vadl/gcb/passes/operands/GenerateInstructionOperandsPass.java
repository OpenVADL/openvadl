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

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.operands.model.GcbConstantOperand;
import vadl.gcb.passes.operands.model.GcbInstructionBareSymbolOperand;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.gcb.passes.operands.model.GcbInstructionIndexedRegisterFileOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.gcb.passes.operands.model.InstructionOperandNamePrintable;
import vadl.gcb.passes.pseudo.PseudoFuncParamNode;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.CompilerInstruction;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.HasRegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;

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
    machineInstructions(passResults, viam);
    pseudoInstructions(viam);
    compilerInstructions(viam);

    return null;
  }

  private void machineInstructions(PassResults passResults, Specification viam) {
    var snapshots =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);
    for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
      var snapshot = ensureNonNull(snapshots.get(instruction),
          () -> Diagnostic.error("Cannot find snapshot for instruction", instruction.location()));
      var outputOperands = getTableGenOutputOperands(snapshot);
      var inputOperands = getTableGenInputOperands(outputOperands, snapshot);

      var ctx = new InstructionOperandsCtx(inputOperands, outputOperands);
      instruction.attachExtension(ctx);
    }
  }

  private void pseudoInstructions(Specification viam) {
    for (var pseudoInstruction : viam.isa().orElseThrow().ownPseudoInstructions()) {
      attachContextToCompilerInstruction(pseudoInstruction);
    }
  }

  private void compilerInstructions(Specification viam) {
    var abi = viam.abi().orElseThrow();
    for (var compilerInstruction : Stream.concat(abi.constantSequences().stream(),
        abi.registerAdjustmentSequences().stream()).toList()) {
      attachContextToCompilerInstruction(compilerInstruction);
    }
  }

  private void attachContextToCompilerInstruction(CompilerInstruction compilerInstruction) {
    var totalInputs = new ArrayList<GcbInstructionOperand>();
    var totalOutputs = new ArrayList<GcbInstructionOperand>();

    for (var callNode : compilerInstruction.behavior().getNodes(InstrCallNode.class).toList()) {
      var instruction = callNode.target();
      var instructionBehavior = instruction.behavior().copy();
      replaceNodesInBehavior(instructionBehavior, callNode);

      var outputOperands = getTableGenOutputOperands(instructionBehavior);
      var inputOperands = getTableGenInputOperands(outputOperands, instructionBehavior);

      addWithoutDuplicates(totalInputs, inputOperands);
      addWithoutDuplicates(totalOutputs, outputOperands);
    }

    checkIfAllOperandsWereDetected(compilerInstruction, totalOutputs, totalInputs);

    var ctx = new CompilerInstructionOperandsCtx(totalInputs, totalOutputs);
    compilerInstruction.attachExtension(ctx);
  }

  private static void checkIfAllOperandsWereDetected(CompilerInstruction compilerInstruction,
                                                     List<GcbInstructionOperand> totalOutputs,
                                                     List<GcbInstructionOperand> totalInputs) {
    // We have now all the operands for each machine instruction. However, we cannot use them
    // directly as operands for the pseudo instruction. The pseudo instruction has itself a list of
    // operands. We needed the operands from the machine instructions to determine whether they
    // are inputs or outputs.

    // All parameters must be either be an output or input operand.
    var unmatchedOperands = new HashSet<>(List.of(compilerInstruction.parameters()));
    for (var pseudoInstructionParameter : compilerInstruction.parameters()) {
      for (var operand : Stream.concat(totalOutputs.stream(), totalInputs.stream()).toList()) {
        if (operand instanceof InstructionOperandNamePrintable printable
            && printable.name().equals(pseudoInstructionParameter.simpleName())) {
          unmatchedOperands.remove(pseudoInstructionParameter);
        }
      }
    }

    for (var operand : unmatchedOperands) {
      throw Diagnostic.error(
          "Operand is not part of any machine instruction and its usage cannot be determined.",
          compilerInstruction.location().join(operand.location())).build();
    }
  }

  private void addWithoutDuplicates(List<GcbInstructionOperand> dest,
                                    List<GcbInstructionOperand> src) {
    for (var element : src) {
      boolean exists = false;
      for (var needle : dest) {
        if (needle.equals(element)) {
          exists = true;
          break;
        }
      }

      if (!exists) {
        dest.add(element);
      }
    }

  }

  private List<GcbInstructionOperand> getTableGenOutputOperands(Graph behavior) {
    var operands = extractWrites(behavior);
    return operands
        .stream()
        .filter(operand -> !operand.indices().isEmpty()) // must not be register.
        .filter(operand -> {
          // Why?
          // Because LLVM cannot handle static registers in input or output operands.
          // They belong to defs and uses instead.
          return !operand.hasConstantAddress();
        })
        .map(this::map)
        .toList();
  }

  private static List<Node> getInputOperands(Graph graph) {
    // First, the registers
    var x = graph.getNodes(ReadRegTensorNode.class).filter(k -> k.regTensor().isRegisterFile());
    var xx = graph.getNodes(ReadArtificialResNode.class).filter(
        k -> k.resourceDefinition().innerResourceRef() instanceof RegisterTensor tensor
            && tensor.isRegisterFile());
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

    return Stream.concat(Stream.concat(Stream.concat(Stream.concat(x, xx), y), z), u)
        .map(k -> (Node) k).toList();
  }

  /**
   * Extracts the input operands from the {@link Graph}. But it will skip nodes which are
   * already a {@link Node} in the {@code outputOperands}. Because if you have a
   * {@link PseudoInstruction} like {@code ADDI rd, rd, 1} then is the output and one input
   * the same which tablegen will not accept.
   */
  private List<GcbInstructionOperand> getTableGenInputOperands(
      List<GcbInstructionOperand> outputOperands,
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
          } else if (node instanceof ReadArtificialResNode artificialResNode
              && artificialResNode.resourceDefinition()
              .innerResourceRef() instanceof RegisterTensor tensor
              && tensor.isRegisterFile()) {
            return !artificialResNode.hasConstantAddress();
          }
          return true;
        })
        .map(this::map)
        .toList();

    return filterOutputs(outputOperands, inputOperands.stream())
        .toList();
  }

  private GcbInstructionOperand map(Node operand) {
    return switch (operand) {
      case ReadRegTensorNode node when node.regTensor().isRegisterFile() -> mapFrom(node);
      case ReadArtificialResNode node -> mapFrom(node);
      case WriteRegTensorNode node when node.regTensor().isRegisterFile() -> mapFrom(node);
      case WriteArtificialResNode node -> mapFrom(node);
      case FuncParamNode node -> mapFrom(node);
      case FieldAccessRefNode node -> mapFrom(node);
      default -> throw Diagnostic.error(
          "Cannot construct a tablegen instruction operand from the type.",
          operand.location()).build();
    };
  }

  /*
  private GcbInstructionOperand mapFrom(Parameter parameter) {
    return new GcbDefaultInstructionOperand(null, );
  }
   */

  /**
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private GcbInstructionOperand mapFrom(FieldAccessRefNode node) {
    return new GcbInstructionImmediateOperand(node);
  }

  /**
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private GcbInstructionOperand mapFrom(FuncParamNode node) {
    return new GcbInstructionBareSymbolOperand(node,
        node.parameter().simpleName());
  }

  /**
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private GcbInstructionOperand mapFrom(
      ReadRegTensorNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new GcbInstructionRegisterFileOperand(node, field);
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new GcbInstructionIndexedRegisterFileOperand(node, funcParamNode);
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
      return new GcbConstantOperand(constantNode, constantValue);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }


  /**
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private GcbInstructionOperand mapFrom(
      ReadArtificialResNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new GcbInstructionRegisterFileOperand(node, field.formatField());
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new GcbInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else if (node.address() instanceof ConstantNode constantNode) {
      var tensor = (RegisterTensor) node.resourceDefinition().innerResourceRef();
      // The register file has a constant as address.
      // This is ok as long as the value of the register file at the address is also constant.
      // For example, the X0 register in RISC-V which always has a constant value.
      var constraints = Arrays.stream(tensor.constraints()).toList();
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
      return new GcbConstantOperand(constantNode, constantValue);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  private GcbInstructionOperand mapFrom(WriteRegTensorNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new GcbInstructionRegisterFileOperand(node, field);
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new GcbInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  private GcbInstructionOperand mapFrom(WriteArtificialResNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new GcbInstructionRegisterFileOperand(node, field.formatField());
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new GcbInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  /**
   * It is not allowed to have a {@link GcbInstructionOperand} in the input list
   * when it is already in the output list. That's why we compute the {@code outputOperands}
   * first and then filter out the {@code stream} for elements which already present in
   * {@code outputOperands}.
   */
  protected static Stream<GcbInstructionOperand> filterOutputs(
      List<GcbInstructionOperand> outputOperands,
      Stream<GcbInstructionOperand> stream) {
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
            .filter(x -> x instanceof GcbInstructionRegisterFileOperand
                || x instanceof GcbInstructionIndexedRegisterFileOperand)
            .collect(Collectors.toSet());

    return stream
        .filter(
            node -> !visited.contains(node));
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private List<WriteResourceNode> extractWrites(Graph graph) {
    return Stream.concat(graph.getNodes(WriteRegTensorNode.class)
                .filter(e -> e.regTensor().isRegisterFile()),
            graph.getNodes(WriteArtificialResNode.class).filter(
                e -> e.resourceDefinition().innerResourceRef() instanceof RegisterTensor tensor
                    && tensor.isRegisterFile()))
        .toList();
  }

  /**
   * There are two relevant cases.
   * The first is that the {@code argument} is a constant. Then, we do not have to do anything.
   * The second case is when {@link CompilerInstruction} uses an {@code index}. Then, the argument
   * is replaced by a {@link FuncParamNode}. However, we still require to know the index for
   * the pseudo instance expansion. That's why we extend {@link FuncParamNode} with
   * {@link PseudoFuncParamNode} which has an {@code index} property.
   * Here is an example of the index. Note that {@code rs} will be transformed into
   * a {@link PseudoFuncParamNode} when it is replaced.
   * <code>
   * pseudo instruction BGEZ( rs : Index, offset : Bits<12> ) =
   * {
   * BGE{ rs1 = rs, rs2 = 0 as Bits5, imm = offset }
   * }
   * </code>
   */
  protected static ExpressionNode indexArgument(List<ExpressionNode> arguments,
                                                ExpressionNode argument) {
    int index = arguments.indexOf(argument);

    // If the node itself a FuncParamNode ...
    if (argument instanceof FuncParamNode funcParamNode) {
      return funcParamNode.replaceAndDelete(
          new PseudoFuncParamNode(funcParamNode.parameter(), index));
    }

    var children = new ArrayList<FuncParamNode>();
    argument.collectInputsWithChildren(children, FuncParamNode.class);

    for (var child : children) {
      child.replaceAndDelete(new PseudoFuncParamNode(child.parameter(), index));
    }

    return argument;

    /*
    if (argument instanceof FuncParamNode funcParamNode) {
      return new PseudoFuncParamNode(funcParamNode.parameter(), index);
    }

    // or its children ...
    var queue = new ArrayDeque<ExpressionNode>();
    queue.add(argument);

    while (!queue.isEmpty()) {
      var element = queue.removeFirst();
      var inputs = element.inputs().toList();

      for (var child : inputs) {
        if (child instanceof FuncParamNode funcParamNode) {
          element.replaceInput(child, new PseudoFuncParamNode(funcParamNode.parameter(), index));
        } else if (child instanceof ExpressionNode expressionNode) {
          queue.add(expressionNode);
        }
      }
    }

    return argument;
     */

    /*
    var children = new ArrayList<FuncParamNode>();
    if (argument instanceof FuncParamNode funcParamNode) {
      children.add(funcParamNode);
    }

    argument.collectInputsWithChildren(children, FuncParamNode.class);

    for (var child : children) {

    }

    if (argument instanceof FuncParamNode funcParamNode) {
      return new PseudoFuncParamNode(funcParamNode.parameter(), index);
    }
    return argument;
     */
  }

  /**
   * Replace the arguments in the behavior of {@code copiedInstructionBehavior}.
   */
  public static void replaceNodesInBehavior(Graph copiedInstructionBehavior,
                                            InstrCallNode callNode) {
    Streams.zip(callNode.getParamFields().stream(), callNode.arguments().stream(),
            Pair::new)
        .forEach(app -> {
          var formatField = app.left();
          var argument = indexArgument(callNode.arguments(), app.right());

          var fields =
              Stream.concat(
                  copiedInstructionBehavior.getNodes(FieldRefNode.class),
                  copiedInstructionBehavior.getNodes(FieldAccessRefNode.class)).toList();

          fields
              .stream()
              .filter(x -> {
                if (x instanceof FieldRefNode fieldRefNode) {
                  return fieldRefNode.formatField().equals(formatField);
                } else if (x instanceof FieldAccessRefNode fieldAccessRefNode) {
                  return fieldAccessRefNode.fieldAccess().fieldRefs().contains(formatField);
                }
                return false;
              })
              .forEach(occurrence -> {
                // Edge case:
                // When we have the following pseudo instruction. Note that "r1" is replaced
                // by a constant. Sometimes, we need to create instruction selectors in TableGen,
                // and it requires a variable. However, if we replace the field by a constant
                // we lose the name of the variable because we have no field anymore.
                // {
                //     JALR{ rs1 = 1 as Bits5, rd = 0 as Bits5, imm = 0 as Bits12 }
                // }

                if (argument instanceof ConstantNode constantNode) {
                  // The constantNode tells me that it will be used as a register index.

                  // Go over the usages to emit warnings.
                  // We need the usage because we need to find out what the register file
                  // to check for constraints.
                  occurrence.usages()
                      .filter(node -> (node instanceof HasRegisterTensor x && x.hasRegisterFile()))
                      .forEach(node -> {
                        var cast = (HasRegisterTensor) node;

                        var constraintValue =
                            Arrays.stream(cast.registerTensor().constraints()).filter(
                                c -> c.indices().getFirst().intValue()
                                    == constantNode.constant().asVal().intValue()).findFirst();

                        if (constraintValue.isEmpty()) {
                          DeferredDiagnosticStore.add(Diagnostic.warning(
                              "There is no constraint value for this register. "
                                  +
                                  "Therefore, we cannot generate instruction selectors for it.",
                              occurrence.location()).build());
                        }
                      });

                  occurrence.replaceAndDelete(argument.copy());

                  // After the replacement, we can check whether we have a write node with
                  // constant node as address which has a constraint. If that's the case, then we
                  // can remove the side effect.
                  occurrence.usages()
                      .filter(node -> node instanceof WriteRegTensorNode writeRegTensorNode
                          && writeRegTensorNode.regTensor().isRegisterFile()
                          && writeRegTensorNode.hasConstantAddress()
                          // Check if there is a constraint for this register index.
                          && Arrays.stream(writeRegTensorNode.regTensor().constraints())
                          .anyMatch(constraint -> constraint.indices().getFirst().intValue()
                              == constantNode.constant().asVal().intValue()))
                      .forEach(Node::safeDelete);
                } else {
                  occurrence.replaceAndDelete(argument.copy());
                }
              });
        });
  }
}
