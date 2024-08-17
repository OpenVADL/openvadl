package vadl.lcb.tablegen.decorators;

import java.io.StringWriter;
import vadl.viam.Specification;

/**
 * Writes all the immediate operands into the TableGenFile.
 */
public class TableGenWithImmediateOperandsDecorator extends TableGenAbstractDecorator {

  public TableGenWithImmediateOperandsDecorator(TableGenAbstractDecorator parent) {
    super(parent);
  }

  @Override
  public void render(StringWriter writer, Specification specification) {
    if (parent != null) {
      parent.render(writer, specification);
    }
  }
}
