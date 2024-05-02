package vadl.types;

/**
 * A Boolean which can only hold true/false.
 */
public class BoolType extends Type {
  @Override
  public String name() {
    return "Bool";
  }

  @Override
  public boolean equals(Object obj) {
    return obj.getClass() == BoolType.class;
  }

  @Override
  public int hashCode() {
    return BoolType.class.hashCode();
  }
}
