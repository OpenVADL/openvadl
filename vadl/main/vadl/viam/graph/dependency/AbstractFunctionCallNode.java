package vadl.viam.graph.dependency;

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphEdgeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * An abstract function call node that has a list of arguments as input.
 * A concrete subtype is the {@link BuiltInCall}.
 */
public abstract class AbstractFunctionCallNode extends ExpressionNode {

  @Input
  protected NodeList<ExpressionNode> args;

  public AbstractFunctionCallNode(NodeList<ExpressionNode> args, Type type) {
    super(type);
    this.args = args;
  }

  public NodeList<ExpressionNode> arguments() {
    return args;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(args);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphEdgeVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    args = args.stream()
        .map((e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }
}