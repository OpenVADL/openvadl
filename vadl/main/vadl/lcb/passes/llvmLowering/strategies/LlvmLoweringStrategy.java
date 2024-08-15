package vadl.lcb.passes.llvmLowering.strategies;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.tablegen.model.TableGenInstruction;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Defines how a {@link Instruction} will be lowered to {@link TableGenInstruction}.
 */
public interface LlvmLoweringStrategy {

  /**
   * Checks whether the given {@link Instruction} is lowerable with this strategy.
   */
  boolean isApplicable(Map<Instruction, InstructionLabel> matching,
                       Instruction instruction);

  /**
   * Generate a lowering result for the given {@link Graph}.
   * If it is not lowerable then return {@link Optional#empty()}.
   */
  Optional<LlvmLoweringPass.LlvmLoweringIntermediateResult> lower(Identifier instructionIdentifier,
                                                                  Graph behavior);

  /**
   * LLvm's TableGen cannot work with control flow. So if statements and other constructs are not
   * supported.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  default boolean checkIfNoControlFlow(Graph behavior) {
    return behavior.getNodes(ControlNode.class)
        .allMatch(x -> x instanceof AbstractBeginNode || x instanceof EndNode); // exceptions
  }

  /**
   * Some dataflow nodes are not lowerable. This function checks whether the {@code behavior}
   * contains these.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  default boolean checkIfNotAllowedDataflowNodes(Graph behavior) {
    return behavior.getNodes(DependencyNode.class)
        .noneMatch(x -> x instanceof FuncParamNode);
  }

  static List<TableGenInstructionOperand> getTableGenOutputOperands(Graph graph) {
    return getOutputOperands(graph)
        .stream().map(operand -> {
          var address = (FieldRefNode) operand.address();

          if (address == null || address.formatField() == null) {
            throw new ViamError("address must not be null");
          }

          return new TableGenInstructionOperand(operand.registerFile().name(),
              address.formatField().identifier.simpleName());
        })
        .toList();
  }

  static List<TableGenInstructionOperand> getTableGenInputOperands(Graph graph) {
    return getInputOperands(graph)
        .stream()
        .map(LlvmLoweringStrategy::generateTableGenInputOutput)
        .toList();
  }

  /**
   * Generate {@link TableGenInstructionOperand} which looks like "X:$lhs" for TableGen.
   */
  static TableGenInstructionOperand generateTableGenInputOutput(Node operand) {
    if (operand instanceof ReadRegFileNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof ReadRegNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof LlvmFieldAccessRefNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof FuncCallNode node) {
      return generateInstructionOperand(node);
    } else {
      throw new ViamError("Input operand not supported yet: " + operand);
    }
  }

  static TableGenInstructionOperand generateInstructionOperand(ReadRegNode node) {
    return new TableGenInstructionOperand(node.register().name(),
        node.register().identifier.simpleName());
  }

  static TableGenInstructionOperand generateInstructionOperand(ReadRegFileNode node) {
    var address = (FieldRefNode) node.address();
    return new TableGenInstructionOperand(node.registerFile().name(),
        address.formatField().identifier.simpleName());
  }

  static TableGenInstructionOperand generateInstructionOperand(LlvmFieldAccessRefNode node) {
    return new TableGenInstructionOperand(node.immediateOperand().getFullName(),
        node.fieldAccess().identifier.simpleName());
  }

  static TableGenInstructionOperand generateInstructionOperand(FuncCallNode node) {
    return new TableGenInstructionOperand(node.function().identifier.lower(),
        node.function().identifier.simpleName());
  }

  /**
   * Most instruction's behaviors have inputs. Those are the results which the instruction requires.
   */
  private static List<Node> getInputOperands(Graph graph) {
    return Stream.concat(Stream.concat(graph.getNodes(ReadResourceNode.class),
            graph.getNodes(FieldAccessRefNode.class)), graph.getNodes(FuncCallNode.class))
        .map(x -> (Node) x).toList();
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private static List<WriteRegFileNode> getOutputOperands(Graph graph) {
    return graph.getNodes(WriteRegFileNode.class).toList();
  }
}
