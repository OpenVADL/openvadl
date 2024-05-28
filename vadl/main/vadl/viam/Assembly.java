package vadl.viam;

import vadl.types.Type;

/**
 * The Assembly definition of an instruction in a VADL specification.
 */
public class Assembly extends Definition {

  private final Function function;

  /**
   * Creates an Assembly object with the specified identifier and arguments.
   *
   * @param identifier the identifier of the Assembly definition
   * @param function   the function to create an assembly string
   */
  public Assembly(Identifier identifier, Function function) {
    super(identifier);

    this.function = function;

    verify();
  }

  @Override
  public void verify() {
    super.verify();
    ensure(function.returnType().equals(Type.string()),
        "Assembly function does not return a String, but %s", function.returnType());
  }

  public Function function() {
    return function;
  }
}
