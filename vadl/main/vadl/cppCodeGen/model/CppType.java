package vadl.cppCodeGen.model;

import vadl.types.Type;

/**
 * Indicates that the type is a cpp type.
 */
public class CppType extends Type {
  protected final String typeName;
  private final boolean isCopy;
  private final boolean isConst;

  /**
   * Constructor.
   */
  public CppType(String typeName, boolean isCopy, boolean isConst) {
    this.typeName = typeName;
    this.isCopy = isCopy;
    this.isConst = isConst;
  }

  @Override
  public String name() {
    return typeName;
  }

  public String lower() {
    return String.format("%s %s%s", isConst ? "const" : "", typeName, isCopy ? "" : "&");
  }
}
