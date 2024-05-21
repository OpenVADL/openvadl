package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Format;

/**
 * A format field reference that may be used as parameter to an instruction.
 *
 * @see Format.Field
 */
public class InstrParamNode extends ParamNode {

  @DataValue
  protected Format.Field formatField;


  /**
   * Constructs a new InstrParamNode object with the given format field and data type.
   * The type of the formatField must be implicitly cast-able to the given type of the
   * node.
   *
   * @param formatField the format field of the instruction parameter
   * @param type        the data type of the instruction parameter
   */
  public InstrParamNode(Format.Field formatField, DataType type) {
    super(type);

    ensure(formatField.type().canBeCastTo(type),
        "Format field type cannot be cast to node type: %s vs %s",
        formatField.type(), type);

    this.formatField = formatField;
  }

  public Format.Field formatField() {
    return formatField;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(formatField);
  }
}
