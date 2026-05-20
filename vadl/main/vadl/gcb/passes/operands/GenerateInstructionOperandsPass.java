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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.RenamedFieldRefNode;
import vadl.gcb.passes.operands.model.GcbConstantOperand;
import vadl.gcb.passes.operands.model.GcbDefaultInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionBareSymbolOperand;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.gcb.passes.operands.model.GcbInstructionIndexedRegisterFileOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.gcb.passes.operands.model.InstructionOperandNamePrintable;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
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
import vadl.viam.graph.IsInstructionOperand;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DynSliceNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.OperationForAllNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.StructGetFieldNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.UnaryNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;
import vadl.viam.passes.functionInliner.Inliner;

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
      var inputOperands = getTableGenInputOperandsForMachineInstructions(outputOperands, snapshot);

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
      var inputOperands =
          getTableGenInputOperandsForPseudoInstructions(compilerInstruction,
              outputOperands,
              instructionBehavior);

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
    // Now, we have all the operands for each machine instruction. However, we cannot use them
    // directly as operands for the pseudo instruction. The pseudo instruction has itself a list of
    // operands that is defined in the specification. We needed the operands from the machine 
    // instructions to determine whether they are inputs or outputs.

    // All parameters must be either be an output or input operand. We are matching the
    // `pseudoInstructionParameter` with the `operand` that was detected.
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
          operand.location()).build();
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
        .distinct()
        .toList();
  }

  private static List<Node> getInputOperands(Graph graph) {
    // There are different use cases for both normal instructions and pseudo instructions.

    // First, the registers
    var x = graph.getNodes(ReadRegTensorNode.class).filter(k -> k.regTensor().isRegisterFile())
        .toList();
    var xx = graph.getNodes(ReadArtificialResNode.class).filter(
        k -> k.resourceDefinition().innerResourceRef() instanceof RegisterTensor tensor
            && tensor.isRegisterFile()).toList();

    // Then, immediates
    var y = graph.getNodes(FieldAccessRefNode.class).toList();

    var z = graph.getNodes(FuncCallNode.class).flatMap(
            funcCallNode -> funcCallNode.function().behavior().getNodes(FuncParamNode.class))
        .toList();

    return concat(x.stream(), xx.stream(), y.stream(), z.stream()).toList();
  }

  private static List<Node> getInputOperandsForPseudoInstructions(CompilerInstruction instruction,
                                                                  Graph graph) {
    var handler = new PseudoNodeOperandCollector(instruction);

    graph.getNodes(SideEffectNode.class).forEach(sideEffectNode -> {
      if (sideEffectNode instanceof WriteRegTensorNode writeRegTensorNode) {
        PseudoNodeOperandCollectorDispatcher.dispatch(handler, writeRegTensorNode.condition());
        PseudoNodeOperandCollectorDispatcher.dispatch(handler, writeRegTensorNode.value());
        // We don't handle the indices because they are considered output operands.
      } else if (sideEffectNode instanceof WriteArtificialResNode writeArtificialResNode) {
        PseudoNodeOperandCollectorDispatcher.dispatch(handler, writeArtificialResNode.condition());
        PseudoNodeOperandCollectorDispatcher.dispatch(handler, writeArtificialResNode.value());
        // We don't handle the indices because they are considered output operands.
      }
    });

    return handler.operands();
  }

  private static Stream<Node> concat(Stream<? extends Node>... streams) {
    return Streams.concat(streams);
  }

  /**
   * Extracts the input operands from the {@link Graph}. But it will skip nodes which are
   * already a {@link Node} in the {@code outputOperands}. Because if you have a
   * {@link PseudoInstruction} like {@code ADDI rd, rd, 1} then is the output and one input
   * the same which tablegen will not accept.
   */
  private List<GcbInstructionOperand> getTableGenInputOperandsForMachineInstructions(
      List<GcbInstructionOperand> outputOperands,
      Graph graph) {

    var inputOperands = getInputOperands(graph)
        .stream()
        .filter(GenerateInstructionOperandsPass::checkWhetherNodeCanBeOperand)
        .map(this::map)
        .toList();

    return filterOutputs(outputOperands, inputOperands.stream())
        .distinct()
        .toList();
  }

  /**
   * Extracts the input operands from the {@link Graph}. But it will skip nodes which are
   * already a {@link Node} in the {@code outputOperands}. Because if you have a
   * {@link PseudoInstruction} like {@code ADDI rd, rd, 1} then is the output and one input
   * the same which tablegen will not accept.
   */
  private List<GcbInstructionOperand> getTableGenInputOperandsForPseudoInstructions(
      CompilerInstruction instruction,
      List<GcbInstructionOperand> outputOperands,
      Graph graph) {

    var inputOperands = getInputOperandsForPseudoInstructions(instruction, graph)
        .stream()
        .filter(GenerateInstructionOperandsPass::checkWhetherNodeCanBeOperand)
        .map(this::map)
        .toList();

    return filterOutputs(outputOperands, inputOperands.stream())
        .toList();
  }

  private static boolean checkWhetherNodeCanBeOperand(Node node) {
    // Why?
    // Because LLVM cannot handle static registers in input or output operands.
    // They belong to defs and uses instead.
    if (node instanceof IsInstructionOperand operandCandidate) {
      return operandCandidate.canBeInstructionOperand();
    } else {
      return node instanceof FuncParamNode || node instanceof FieldAccessRefNode;
    }
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
    if (node.address() instanceof FieldRefNode fieldRefNode) {
      return new GcbInstructionRegisterFileOperand(node, fieldRefNode.formatField());
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new GcbInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else if (node.address() instanceof ConstantNode constantNode) {
      // The register file has a constant as address.
      // This is ok as long as the value of the register file at the address is also constant.
      // For example, the X0 register in RISC-V which always has a constant value.
      var constraints = node.regTensor().constraints();
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
      // The register file has a constant as address.
      // This is ok as long as the value of the register file at the address is also constant.
      // For example, the X0 register in RISC-V which always has a constant value.
      var constraints = node.getAllConstraintsRecursively();
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
            .map(x -> ((GcbDefaultInstructionOperand) x).name())
            .collect(Collectors.toSet());

    return stream
        .filter(
            node -> node instanceof GcbDefaultInstructionOperand defaultInstructionOperand
                && !visited.contains(defaultInstructionOperand.name()));
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private List<WriteResourceNode> extractWrites(Graph graph) {
    return Stream.concat(graph.getNodes(WriteRegTensorNode.class)
                .filter(e -> e.regTensor().isRegisterFile())
                .map(x -> (WriteResourceNode) x),
            graph.getNodes(WriteArtificialResNode.class).filter(
                e -> e.resourceDefinition().innerResourceRef() instanceof RegisterTensor tensor
                    && tensor.isRegisterFile()))
        .toList();
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
          var argument = app.right();

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
                            cast.registerTensor().constraints().stream()
                                .filter(c -> c.indices().getFirst().intValue()
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
                          && writeRegTensorNode.regTensor().constraints().stream()
                          .anyMatch(constraint -> constraint.indices().getFirst().intValue()
                              == constantNode.constant().asVal().intValue()))
                      .forEach(Node::safeDelete);
                } else {
                  occurrence.replaceAndDelete(argument.copy());
                }
              });
        });
    Inliner.inlineFuncs(copiedInstructionBehavior, Inliner.InliningMode.WithRelocations);
  }
}

@DispatchFor(value = ExpressionNode.class,
    include = {"vadl.viam"}
)
class PseudoNodeOperandCollector {
  private final CompilerInstruction compilerInstruction;
  private final List<Node> operands = new ArrayList<>();

  PseudoNodeOperandCollector(CompilerInstruction compilerInstruction) {
    this.compilerInstruction = compilerInstruction;
  }

  public List<Node> operands() {
    return operands;
  }

  @Handler
  protected void handle(FuncParamNode node) {
    operands.add(node);
  }

  @Handler
  protected void handle(UnaryNode node) {
    PseudoNodeOperandCollectorDispatcher.dispatch(this, node.value());
  }


  @Handler
  protected void handle(TensorNode node) {

  }

  @Handler
  protected void handle(AsmBuiltInCall node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  protected void handle(ReadArtificialResNode node) {
    if (node.resourceDefinition().innerResourceRef() instanceof RegisterTensor tensor
        && tensor.isRegisterFile()) {
      // We do not want renamed field ref nodes.
      // Example: MOVKWPos16; here we would have `rd` in the outputs and `rd_1` in the input
      if (!(node.address() instanceof RenamedFieldRefNode)) {
        operands.add(node);
      }
    }
  }

  @Handler
  protected void handle(ReadRegTensorNode node) {
    if (node.regTensor().isRegisterFile()) {
      // We do not want renamed field ref nodes.
      // Example: MOVKWPos16; here we would have `rd` in the outputs and `rd_1` in the input
      if (!(node.address() instanceof RenamedFieldRefNode)) {
        operands.add(node);
      }
    }
  }

  @Handler
  protected void handle(BuiltInCall node) {
    for (var arg : node.arguments()) {
      PseudoNodeOperandCollectorDispatcher.dispatch(this, arg);
    }
  }


  @Handler
  protected void handle(StructGetFieldNode node) {
    PseudoNodeOperandCollectorDispatcher.dispatch(this, node.expression());
  }

  @Handler
  protected void handle(ConstantNode node) {

  }

  @Handler
  protected void handle(FuncCallNode node) {
    throw Diagnostic.error("Should be already inlined",
        node.location().join(compilerInstruction.location())).build();
  }

  @Handler
  protected void handle(ReadStageOutputNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  protected void handle(ForIdxNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  protected void handle(FoldNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  protected void handle(OperationForAllNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  protected void handle(OperationForAllNode.Index node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  protected void handle(LetNode node) {
    PseudoNodeOperandCollectorDispatcher.dispatch(this, node.expression());
  }

  @Handler
  protected void handle(SliceNode node) {
    PseudoNodeOperandCollectorDispatcher.dispatch(this, node.value());
  }

  @Handler
  protected void handle(DynSliceNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  protected void handle(ReadMemNode node) {
    PseudoNodeOperandCollectorDispatcher.dispatch(this, node.address());
  }

  @Handler
  protected void handle(MiaBuiltInCall node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  protected void handle(FieldRefNode node) {
    throw Diagnostic.error("This field should have been replaced by pseudo instruction argument.",
        node.location()).build();
  }

  @Handler
  protected void handle(FieldAccessRefNode node) {

  }

  @Handler
  protected void handle(LabelNode node) {

  }

  @Handler
  protected void handle(SelectNode node) {
    PseudoNodeOperandCollectorDispatcher.dispatch(this, node.condition());
    PseudoNodeOperandCollectorDispatcher.dispatch(this, node.trueCase());
    PseudoNodeOperandCollectorDispatcher.dispatch(this, node.falseCase());
  }

  @Handler
  protected void handle(ReadSignalNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }
}
