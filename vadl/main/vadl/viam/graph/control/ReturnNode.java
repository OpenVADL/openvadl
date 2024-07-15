package vadl.viam.graph.control;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.oop.OopGeneratable;
import vadl.oop.SymbolTable;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;


/**
 * Represents the end node of the control flow graph of a pure function.
 */
public class ReturnNode extends AbstractEndNode implements OopGeneratable {

  @Input
  public ExpressionNode value;

  public ReturnNode(ExpressionNode value, NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
    this.value = value;
  }

  public ReturnNode(ExpressionNode value) {
    super(new NodeList<>());
    this.value = value;
  }

  public Type returnType() {
    return value.type();
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
  }

  @Override
  public Node copy() {
    return new ReturnNode((ExpressionNode) value.copy());
  }

  @Override
  public Node shallowCopy() {
    return new ReturnNode(value);
  }

  @Override
  public String generateOopExpression(SymbolTable symbolTable) {
    return "return " + value.generateOopExpression(symbolTable);
  }
}
