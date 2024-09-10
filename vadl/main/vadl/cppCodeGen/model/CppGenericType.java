package vadl.cppCodeGen.model;

import vadl.types.Type;

public class CppGenericType extends Type {
  private final String generic;

  private final String container;

  public CppGenericType(String container, String generic) {
    this.container = container;
    this.generic = generic;
  }

  @Override
  public String name() {
    return generic;
  }
}
