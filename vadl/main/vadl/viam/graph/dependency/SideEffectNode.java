package vadl.viam.graph.dependency;

import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.Resource;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a node with side effect. Such nodes are dependencies
 * of {@link vadl.viam.graph.control.AbstractEndNode}.
 *
 * <p>A side effect has a {@code condition} under which it takes affect/is executed.
 * This condition is resolved during the
 * {@link vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass} and
 * therefore is not available until the pass was executed.</p>
 */
public abstract class SideEffectNode extends DependencyNode {

  @Input
  @Nullable
  protected ExpressionNode condition;

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

  public @Nullable ExpressionNode nullableCondition() {
    return condition;
  }

  abstract public Resource resourceDefinition();

  /**
   * Sets the condition of the side effect.
   * The condition defines under what condition the side effect takes place.
   */
  public void setCondition(ExpressionNode condition) {
    ensure(condition.type().isTrivialCastTo(Type.bool()), "Condition must be a boolean but was %s",
        condition);
    updateUsageOf(this.condition, condition);
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
