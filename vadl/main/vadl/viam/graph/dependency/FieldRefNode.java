package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.oop.SymbolTable;
import vadl.types.DataType;
import vadl.viam.Format;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;

/**
 * A node reference an instruction's format field.
 *
 * @see Format.Field
 */
public class FieldRefNode extends ParamNode {

  @DataValue
  protected Format.Field formatField;


  /**
   * Constructs a new {@link FieldRefNode} object with the given format field and data type.
   * The type of the formatField must be implicitly cast-able to the given type of the
   * node.
   *
   * @param formatField the format field of the instruction parameter
   * @param type        the data type of the instruction parameter
   */
  public FieldRefNode(Format.Field formatField, DataType type) {
    super(type);

    this.formatField = formatField;
  }

  public Format.Field formatField() {
    return formatField;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(formatField.type().canBeCastTo((DataType) type()),
        "Format field type cannot be cast to node type: %s vs %s",
        formatField.type(), type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(formatField);
  }

  @Override
  public Node copy() {
    return new FieldRefNode(formatField, (DataType) type());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public String generateOopExpression() {
    throw new RuntimeException("not implemented");
  }
}
