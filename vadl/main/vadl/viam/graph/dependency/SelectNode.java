package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.BoolType;
import vadl.viam.graph.GraphNodeVisitor;
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
    this.condition = condition;
    this.trueCase = trueCase;
    this.falseCase = falseCase;

    ensure(trueCase.type().isTrivialCastTo(falseCase.type()),
        "True and false case must have the same type. %s vs %s", trueCase.type(), falseCase.type());
    ensure(condition.type() instanceof BoolType, "Condition must have type Bool");
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

  @Override
  public Node copy() {
    return new SelectNode((ExpressionNode) condition.copy(),
        (ExpressionNode) trueCase.copy(),
        (ExpressionNode) falseCase.copy());
  }

  @Override
  public Node shallowCopy() {
    return new SelectNode(condition, trueCase, falseCase);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  public ExpressionNode condition() {
    return condition;
  }

  public ExpressionNode trueCase() {
    return trueCase;
  }

  public ExpressionNode falseCase() {
    return falseCase;
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append("(");
    condition.prettyPrint(sb);
    sb.append(" ? ");
    trueCase.prettyPrint(sb);
    sb.append(" : ");
    falseCase.prettyPrint(sb);
    sb.append(")");
  }
}
