package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.StageOutput;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Read from stage output.
 */
public class ReadStageOutputNode extends ExpressionNode {

  @DataValue
  protected StageOutput stageOutput;

  public ReadStageOutputNode(StageOutput stageOutput) {
    super(stageOutput.type());
    this.stageOutput = stageOutput;
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
  public ExpressionNode copy() {
    return new ReadStageOutputNode(stageOutput);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
