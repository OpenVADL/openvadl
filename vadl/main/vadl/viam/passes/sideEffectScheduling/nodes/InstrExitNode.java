package vadl.viam.passes.sideEffectScheduling.nodes;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.dependency.WriteResourceNode;

public class InstrExitNode extends DirectionalNode {

  @Input
  private WriteResourceNode pcWrite;

  public InstrExitNode(WriteResourceNode pcWrite) {
    this.pcWrite = pcWrite;
  }

  public WriteResourceNode pcWrite() {
    return pcWrite;
  }

  @Override
  public Node copy() {
    return new InstrExitNode(pcWrite.copy(WriteResourceNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new InstrExitNode(pcWrite);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // not used
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(pcWrite);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    pcWrite = visitor.apply(this, pcWrite, WriteResourceNode.class);
  }
}
