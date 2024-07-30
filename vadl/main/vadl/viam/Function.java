package vadl.viam;

import java.util.stream.Stream;
import vadl.types.ConcreteRelationType;
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
  private Graph behavior;
  private final Type returnType;
  private final Parameter[] parameters;

  /**
   * Creates a new Function with the specified identifier, parameters, and return type.
   *
   * <p>An empty behaviour-graph is automatically created.</p>
   *
   * @param identifier The identifier of the Function.
   * @param parameters The parameters of the Function.
   * @param returnType The return type of the Function.
   */
  public Function(Identifier identifier,
                  Parameter[] parameters,
                  Type returnType) {
    this(identifier, parameters, returnType, new Graph(identifier.name()));
  }

  /**
   * Creates a new Function with the specified identifier, parameters, and return type.
   *
   * @param identifier The identifier of the Function.
   * @param parameters The parameters of the Function.
   * @param returnType The return type of the Function.
   * @param behavior   The behavior of the Function.
   */
  public Function(Identifier identifier,
                  Parameter[] parameters,
                  Type returnType,
                  Graph behavior) {
    super(identifier);
    this.behavior = behavior;
    this.returnType = returnType;
    this.parameters = parameters;
  }

  public Graph behavior() {
    return behavior;
  }

  public Parameter[] parameters() {
    return parameters;
  }

  public void setBehavior(Graph graph) {
    this.behavior = graph;
  }

  public ConcreteRelationType signature() {
    return Type.concreteRelation(Stream.of(parameters).map(Parameter::type).toList(), returnType);
  }

  public Type returnType() {
    return returnType;
  }

  @Override
  public String toString() {
    return name() + signature();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
