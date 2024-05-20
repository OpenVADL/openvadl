package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Format;

/**
 * A format field reference that may be used as parameter to an instruction.
 */
public class InstrParamNode extends ParamNode {

  @DataValue
  Format.Field formatField;

  public InstrParamNode(Format.Field formatField, DataType type) {
    super(type);

    ensure(formatField.type().canBeCastTo(type),
        "Format field type cannot be cast to node type: %s vs %s",
        formatField.type(), type);

    this.formatField = formatField;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(formatField);
  }
}
