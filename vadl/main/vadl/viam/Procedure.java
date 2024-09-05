package vadl.viam;

import java.util.List;
import vadl.utils.GraphUtils;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ProcedureEndNode;

public class Procedure extends Definition implements DefProp.WithBehavior {

  private final Parameter[] parameters;
  private final Graph behavior;

  public Procedure(Identifier identifier, Parameter[] parameters, Graph behavior) {
    super(identifier);
    this.parameters = parameters;
    this.behavior = behavior;
    this.behavior.setParentDefinition(this);
  }

  public Parameter[] parameters() {
    return parameters;
  }

  @Override
  public void verify() {
    super.verify();

    GraphUtils.getSingleNode(behavior, ProcedureEndNode.class);

    behavior.verify();
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
