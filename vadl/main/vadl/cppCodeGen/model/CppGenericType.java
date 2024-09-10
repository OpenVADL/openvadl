package vadl.cppCodeGen.model;

public class CppGenericType extends CppType {
  private final CppType generic;

  public CppGenericType(String container, CppType generic) {
    super(container, false, false);
    this.generic = generic;
  }

  @Override
  public String name() {
    return generic.name();
  }

  public String lower() {
    return String.format("%s<%s>", typeName, generic.lower());
  }
}
