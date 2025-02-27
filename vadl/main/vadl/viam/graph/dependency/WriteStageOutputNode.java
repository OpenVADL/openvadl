package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.Resource;
import vadl.viam.StageOutput;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Write value from expression node to stage output.
 */
public class WriteStageOutputNode extends SideEffectNode {

  @DataValue
  protected StageOutput stageOutput;

  @Input
  protected ExpressionNode value;

  public WriteStageOutputNode(StageOutput stageOutput, ExpressionNode value) {
    this.stageOutput = stageOutput;
    this.value = value;
  }

  public StageOutput stageOutput() {
    return stageOutput;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(stageOutput);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }

  @Override
  public Node copy() {
    return new WriteStageOutputNode(stageOutput, value);
  }

  @Override
  public Node shallowCopy() {
    return new WriteStageOutputNode(stageOutput, value.copy());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
