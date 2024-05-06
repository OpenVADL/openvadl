package vadl.viam.graph.dependency;

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

public abstract class AbstractFunctionCall extends ExpressionNode {

  @Input
  protected NodeList<ExpressionNode> args;

  AbstractFunctionCall(NodeList<ExpressionNode> args, Type type) {
    super(type);
    this.args = args;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(args);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    args = args.stream()
        .map((e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }
}