package vadl.viam;

import java.util.List;
import vadl.types.Type;
import vadl.viam.graph.Graph;

public class Function extends Definition {
  private final Graph behaviour;
  private final Type returnType;
  private final List<Parameter> arguments;

  public Function(Identifier identifier, List<Parameter> arguments,
                  Type returnType) {
    super(identifier);
    this.behaviour = new Graph(identifier.name());
    this.returnType = returnType;
    this.arguments = arguments;
  }

  public Graph behaviour() {
    return behaviour;
  }

  public List<Parameter> arguments() {
    return arguments;
  }

  public Type returnType() {
    return returnType;
  }
}
