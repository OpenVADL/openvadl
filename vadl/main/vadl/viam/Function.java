package vadl.viam;

import java.util.List;
import java.util.stream.Collectors;
import vadl.types.Type;
import vadl.viam.graph.Graph;

/**
 * Represents a Function in a VADL specification.
 *
 * <p>
 * A Function is a type of Definition that has a behavior (body), return type, and arguments.
 * </p>
 */
public class Function extends Definition {
  private final Graph behavior;
  private final Type returnType;
  private final List<Parameter> parameters;

  /**
   * Creates a new Function with the specified identifier, parameters, and return type.
   *
   * <p>An empty behaviour-graph is automatically created.</p>
   *
   * @param identifier The identifier of the Function.
   * @param parameters The parameters of the Function.
   * @param returnType The return type of the Function.
   */
  public Function(Identifier identifier, List<Parameter> parameters,
                  Type returnType) {
    super(identifier);
    this.behavior = new Graph(identifier.name());
    this.returnType = returnType;
    this.parameters = parameters;
  }

  public Graph behavior() {
    return behavior;
  }

  public List<Parameter> parameters() {
    return parameters;
  }

  public Type returnType() {
    return returnType;
  }

  @Override
  public String toString() {
    return name() + "(" +
        parameters.stream().map(Object::toString).collect(Collectors.joining(", ")) +
        ") -> " + returnType;
  }
}
