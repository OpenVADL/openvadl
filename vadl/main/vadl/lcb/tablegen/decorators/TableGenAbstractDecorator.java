package vadl.lcb.tablegen.decorators;

import java.io.StringWriter;
import vadl.viam.Specification;

/**
 * Decorator for all the contents of a tablegen file.
 */
public abstract class TableGenAbstractDecorator {
  protected TableGenAbstractDecorator parent;

  public TableGenAbstractDecorator(TableGenAbstractDecorator parent) {
    this.parent = parent;
  }

  /**
   * Writes the content of the decorator into {@code writer}.
   */
  public abstract void render(StringWriter writer, Specification specification);
}
