package vadl.types;

/**
 * A Boolean which can only hold true/false.
 */
public class BoolType extends Type {
  @Override
  public String name() {
    return "Bool";
  }
}
