package vadl.viam;

import java.util.List;
import java.util.stream.Stream;
import vadl.types.ConcreteRelationType;
import vadl.types.Type;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;

/**
 * Represents a Function in a VADL specification.
 *
 * <p>
 * A Function is a type of Definition that has a behavior (body), return type, and arguments.
 * </p>
 */
public class Function extends Definition implements WithBehavior {
  private Graph behavior;
  private Type returnType;
  private Parameter[] parameters;

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

  @Override
  public void verify() {
    super.verify();
    ensure(behavior.isPureFunction(), "The function must be pure (no side effects)");

    var returnNode = behavior().getNodes(ReturnNode.class).findFirst().get();
    ensure(returnNode.value().type() == returnType,
        "The function's behavior does not return same type as signature suggests: %s", returnType);
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

  public void setReturnType(Type returnType) {
    this.returnType = returnType;
  }

  public void setParameters(Parameter[] parameters) {
    this.parameters = parameters;
  }

  @Override
  public String toString() {
    return name() + signature();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public List<Graph> behaviors() {
    return List.of(behavior);
  }
}
