package vadl.viam.graph.dependency;

import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a node with side effect. Such nodes are dependencies
 * of {@link vadl.viam.graph.control.AbstractEndNode}.
 */
public abstract class SideEffectNode extends DependencyNode {

  @Input
  @Nullable
  ExpressionNode condition;

  @Override
  public void verifyState() {
    super.verifyState();
    if (condition != null) {
      ensure(condition.type().isTrivialCastTo(Type.bool()),
          "Condition must be a boolean but was %s",
          condition);
    }
  }

  public ExpressionNode condition() {
    ensure(condition != null, "Condition was expected to be not null.");
    return condition;
  }

  public void setCondition(ExpressionNode condition) {
    ensure(condition.type().isTrivialCastTo(Type.bool()), "Condition must be a boolean but was %s",
        condition);
    updateUsage(this.condition, condition);
    this.condition = condition;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.condition != null) {
      collection.add(condition);
    }
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.applyNullable(this, condition, ExpressionNode.class);
  }
}
