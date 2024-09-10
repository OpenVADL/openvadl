package vadl.cppCodeGen.model;

import vadl.types.Type;

public class CppType extends Type {
  private final String typeName;
  private final boolean isCopy;
  private final boolean isConst;

  public CppType(String typeName, boolean isCopy, boolean isConst) {
    this.typeName = typeName;
    this.isCopy = isCopy;
    this.isConst = isConst;
  }

  @Override
  public String name() {
    return typeName;
  }
}
