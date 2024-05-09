package vadl.viam;

import java.util.List;
import vadl.types.DummyType;

/**
 * The Assembly definition of an instruction in a VADL specification.
 */
public class Assembly extends Definition {

  private final Function function;


  /**
   * Creates an Assembly object with the specified identifier and arguments.
   *
   * @param identifier the identifier of the Assembly definition
   * @param arguments  the list of Parameter objects representing the arguments of the Assembly
   */
  public Assembly(Identifier identifier, List<Parameter> arguments) {
    super(identifier);
    // TODO: Replace by correct type
    this.function = new Function(identifier, arguments, DummyType.INSTANCE);
  }

  public Function function() {
    return function;
  }
}
