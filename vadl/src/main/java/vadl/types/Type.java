package vadl.types;

/**
 * The top type of VADL's type system.
 * All other types extend it.
 */
public abstract class Type {

  /**
   * A readable representation of the type.
   *
   * @return the name of the type
   */
  public abstract String name();

  /**
   * Since the typesystem allows aliases it is sometimes handy to get the acutall underlying type.
   *
   * @return the type, disregarding all aliases.
   */
  public abstract Type concreteType();
}
