package vadl.viam.passes.sideEffectScheduling.nodes;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Represents the exit point of an instruction in the side-effect scheduling pass.
 * This node is a {@link DirectionalNode} that signifies the completion of an instruction,
 * specifically handling the write to the program counter (PC).
 */
public class InstrExitNode extends DirectionalNode {

  /**
   * The write operation to the program counter.
   */
  @Input
  private WriteResourceNode pcWrite;

  /**
   * Constructs an {@code InstrExitNode} with the specified PC write operation.
   *
   * @param pcWrite The {@link WriteResourceNode} representing the write to the program counter.
   */
  public InstrExitNode(WriteResourceNode pcWrite) {
    this.pcWrite = pcWrite;
  }

  /**
   * Returns the {@link WriteResourceNode} associated with this node.
   *
   * @return The PC write operation.
   */
  public WriteResourceNode pcWrite() {
    return pcWrite;
  }

  /**
   * Creates a deep copy of this node, including a copy of the {@code pcWrite} node.
   *
   * @return A new {@code InstrExitNode} that is a deep copy of this node.
   */
  @Override
  public Node copy() {
    return new InstrExitNode(pcWrite.copy(WriteResourceNode.class));
  }

  /**
   * Creates a shallow copy of this node, reusing the same {@code pcWrite} node.
   *
   * @return A new {@code InstrExitNode} that is a shallow copy of this node.
   */
  @Override
  public Node shallowCopy() {
    return new InstrExitNode(pcWrite);
  }

  /**
   * Accepts a graph node visitor. This method is not used in this implementation.
   *
   * @param visitor The visitor to accept.
   * @param <T>     The type of the graph node visitor.
   */
  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // not used
  }

  /**
   * Collects the input nodes of this node into the provided collection.
   * This includes the {@code pcWrite} node.
   *
   * @param collection The list to collect input nodes into.
   */
  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(pcWrite);
  }

  /**
   * Applies the given visitor to this node's inputs in an unsafe manner.
   * This method updates the {@code pcWrite} node by applying the visitor.
   *
   * @param visitor The visitor to apply to the input nodes.
   */
  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    pcWrite = visitor.apply(this, pcWrite, WriteResourceNode.class);
  }
}