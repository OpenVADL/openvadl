package vadl.cppCodeGen.model;

/**
 * Value wrapper for class name.
 */
public record CppClassImplName(String name) {
  public String lower() {
    return name;
  }
}
