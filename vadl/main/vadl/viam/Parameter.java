package vadl.viam;

import vadl.types.Type;

public class Parameter extends Definition {

  private final Type type;

  public Parameter(Identifier identifier, Type type) {
    super(identifier);
    this.type = type;
  }

  public Type type() {
    return type;
  }
}
