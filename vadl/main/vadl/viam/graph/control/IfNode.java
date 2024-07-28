package vadl.viam.graph.control;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;


/**
 * IfNode is a class that represents an if statement in a control flow graph.
 * It extends the ControlSplitNode class, which is a control flow node that
 * causes diverging execution.
 *
 * <p>The IfNode class has the following fields:
 * <li>{@code condition}: An ExpressionNode object that represents the condition of
 * the if statement.</li>
 * <li>{@code trueBranch}: A Node object that represents the true branch of the if statement.</li>
 * <li>{@code falseBranch}: A Node object that represents the false branch of the if statement.</li>
 *
 * <p>This class uses the following annotations:
 * <li>{@code @Input}: Marks the condition field as an input field, pointing to another node.</li>
 * <li>{@code @Successor}: Marks the trueBranch and falseBranch fields as successor node properties
 * of the if statement.</li>
 */
public class IfNode extends ControlSplitNode {

  @Input
  ExpressionNode condition;

  @Successor
  Node trueBranch;

  @Successor
  Node falseBranch;

  /**
   * The constructor to instantiate a IfNode.
   */
  public IfNode(ExpressionNode condition, Node trueBranch, Node falseBranch) {
    this.condition = condition;
    this.trueBranch = trueBranch;
    this.falseBranch = falseBranch;
  }


  @Override
  public Node copy() {
    return new IfNode((ExpressionNode) condition.copy(), trueBranch.copy(), falseBranch.copy());
  }

  @Override
  public void canonicalize() {
    this.condition.canonicalize();
    this.trueBranch.canonicalize();
    this.falseBranch.canonicalize();
  }

  @Override
  public Node shallowCopy() {
    return new IfNode(condition, trueBranch, falseBranch);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s(t: %s, f: %s)".formatted(super.toString(), trueBranch.id, falseBranch.id);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(condition);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.apply(this, condition, ExpressionNode.class);
  }

  @Override
  protected void collectSuccessors(List<Node> collection) {
    super.collectSuccessors(collection);
    collection.add(trueBranch);
    collection.add(falseBranch);
  }

  @Override
  protected void applyOnSuccessorsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnSuccessorsUnsafe(visitor);
    trueBranch = visitor.apply(this, trueBranch);
    falseBranch = visitor.apply(this, falseBranch);
  }
}
