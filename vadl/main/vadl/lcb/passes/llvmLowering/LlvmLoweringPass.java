package vadl.lcb.passes.llvmLowering;

import java.util.List;
import java.util.stream.Stream;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.pass.Pass;
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
 * This is a wrapper class which contains utility functions for the lowering.
 */
public abstract class LlvmLoweringPass extends Pass {
  /**
   * LLvm's TableGen cannot work with control flow. So if statements and other constructs are not
   * supported.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  protected boolean checkIfNoControlFlow(Graph behavior) {
    return behavior.getNodes(ControlNode.class)
        .allMatch(x -> x instanceof AbstractBeginNode || x instanceof EndNode); // exceptions
  }

  /**
   * Some dataflow nodes are not lowerable. This function checks whether the {@code behavior}
   * contains these.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  protected boolean checkIfNotAllowedDataflowNodes(Graph behavior) {
    return behavior.getNodes(DependencyNode.class)
        .noneMatch(x -> x instanceof FuncParamNode);
  }

  protected List<TableGenInstructionOperand> getTableGenOutputOperands(Graph graph) {
    return getOutputOperands(graph)
        .stream().map(operand -> new TableGenInstructionOperand(operand.registerFile().name(),
            operand.nodeName()))
        .toList();
  }

  protected List<TableGenInstructionOperand> getTableGenInputOperands(Graph graph) {
    return getInputOperands(graph)
        .stream()
        .map(operand -> {
          if (operand instanceof ReadRegFileNode node) {
            var address = (FieldRefNode) node.address();
            return new TableGenInstructionOperand(node.registerFile().name(),
                address.formatField().identifier.simpleName());
          } else if (operand instanceof ReadRegNode node) {
            return new TableGenInstructionOperand(node.register().name(),
                operand.nodeName());
          } else if (operand instanceof FieldAccessRefNode node) {
            return new TableGenInstructionOperand(node.fieldAccess().accessFunction().name(),
                operand.nodeName());
          } else if (operand instanceof FuncCallNode node) {
            return new TableGenInstructionOperand(node.function().identifier.lower(),
                node.function().identifier.simpleName());
          } else {
            throw new ViamError("Input operand not supported yet: " + operand);
          }
        })
        .toList();
  }

  /**
   * Most instruction's behaviors have inputs. Those are the results which the instruction requires.
   */
  private List<Node> getInputOperands(Graph graph) {
    return Stream.concat(Stream.concat(graph.getNodes(ReadResourceNode.class),
            graph.getNodes(FieldAccessRefNode.class)), graph.getNodes(FuncCallNode.class))
        .map(x -> (Node) x).toList();
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private List<WriteRegFileNode> getOutputOperands(Graph graph) {
    return graph.getNodes(WriteRegFileNode.class).toList();
  }
}
