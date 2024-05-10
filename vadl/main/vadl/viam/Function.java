package vadl.viam;

import java.util.List;
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
  private final List<Parameter> arguments;

  /**
   * Creates a new Function with the specified identifier, arguments, and return type.
   *
   * <p>An empty behaviour-graph is automatically created.</p>
   *
   * @param identifier The identifier of the Function.
   * @param arguments  The arguments of the Function.
   * @param returnType The return type of the Function.
   */
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
