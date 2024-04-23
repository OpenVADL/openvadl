package vadl.viam.graph.nodes.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * The SelectNode class represents a node in a graph that applies a condition to select
 * between two cases. It extends the ExpressionNode class.
 */
public class SelectNode extends ExpressionNode {

  @Input
  ExpressionNode condition;

  @Input
  ExpressionNode trueCase;
  @Input
  ExpressionNode falseCase;

  /**
   * Constructor to instantiate a select node.
   */
  public SelectNode(ExpressionNode condition, ExpressionNode trueCase, ExpressionNode falseCase) {
    this.condition = condition;
    this.trueCase = trueCase;
    this.falseCase = falseCase;
  }


  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(condition);
    collection.add(trueCase);
    collection.add(falseCase);
  }


  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.apply(this, condition, ExpressionNode.class);
    trueCase = visitor.apply(this, trueCase, ExpressionNode.class);
    falseCase = visitor.apply(this, falseCase, ExpressionNode.class);
  }

  @Override
  public String toString() {
    return "%s(%s, %s)".formatted(super.toString(), trueCase.id, falseCase.id);
  }
}
