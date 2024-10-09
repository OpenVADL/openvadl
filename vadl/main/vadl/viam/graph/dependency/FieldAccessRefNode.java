package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a node that holds a reference to a format field access.
 */
public class FieldAccessRefNode extends ParamNode {

  @DataValue
  protected Format.FieldAccess fieldAccess;

  /**
   * Creates an FieldAccessRefNode object that holds a reference to a format field access.
   *
   * @param fieldAccess the format immediate to be referenced
   */
  public FieldAccessRefNode(Format.FieldAccess fieldAccess, Type type) {
    super(type);

    this.fieldAccess = fieldAccess;
  }

  public Format.FieldAccess fieldAccess() {
    return fieldAccess;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(fieldAccess);
  }

  @Override
  public void verifyState() {
    super.verifyState();

    // TODO: Replace by isTrivialCastTo
    ensure(fieldAccess.type() == type(),
        "Type of fieldAccess can't be trivially cast to node's type. %s vs %s", fieldAccess.type(),
        type());

  }

  @Override
  public Node copy() {
    return new FieldAccessRefNode(fieldAccess, type());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Definition definition() {
    return fieldAccess;
  }
}
