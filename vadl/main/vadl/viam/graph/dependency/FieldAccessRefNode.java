package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.oop.SymbolTable;
import vadl.viam.Format;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;

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
  public FieldAccessRefNode(Format.FieldAccess fieldAccess) {
    super(fieldAccess.type());

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
  public Node copy() {
    return new FieldAccessRefNode(fieldAccess);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public String generateOopExpression(SymbolTable symbolTable) {
    throw new RuntimeException("not implemented exception");
  }
}
