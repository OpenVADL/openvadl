package vadl.viam;

import java.util.List;
import vadl.types.DummyType;

public class Assembly extends Definition {

  private final Function function;

  public Assembly(Identifier identifier, List<Parameter> arguments) {
    super(identifier);
    // TODO: Replace by correct type
    this.function = new Function(identifier, arguments, DummyType.INSTANCE);
  }

  public Function function() {
    return function;
  }
}
