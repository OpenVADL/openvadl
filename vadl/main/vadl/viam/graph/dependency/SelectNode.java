package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.BoolType;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents the If-Expression in a VADL specification.
 * All its cases produce a value and are side effect free.
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
    super(trueCase.type());
    ensure(trueCase.type().equals(falseCase.type()), "True and false case must have the same type");
    ensure(condition.type() instanceof BoolType, "Condition must have type Bool");
    this.condition = condition;
    this.trueCase = trueCase;
    this.falseCase = falseCase;
  }

  @Override
  public Type type() {
    ensure(trueCase.type().equals(falseCase.type()), "True and false case must have the same type");
    return trueCase.type();
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
