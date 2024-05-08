package vadl.viam;

import java.util.List;
import vadl.types.Type;
import vadl.viam.graph.Graph;

public class Function extends Definition {
  private final Graph behavior;
  private final Type returnType;
  private final List<Parameter> arguments;

  public Function(Identifier identifier, List<Parameter> arguments,
                  Type returnType) {
    super(identifier);
    this.behavior = new Graph(identifier.name());
    this.returnType = returnType;
    this.arguments = arguments;
  }

  public Graph behavior() {
    return behavior;
  }

  public List<Parameter> arguments() {
    return arguments;
  }

  public Type returnType() {
    return returnType;
  }
}
