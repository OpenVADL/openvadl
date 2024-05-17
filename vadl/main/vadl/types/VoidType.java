package vadl.types;

/**
 * Represents the void type in VADL's type system.
 *
 * <p>The void type is not visible to the user, but required as some built-ins
 * do return nothing.</p>
 */
public class VoidType extends Type {

  protected VoidType() {
  }

  @Override
  public String name() {
    return "void";
  }
}
