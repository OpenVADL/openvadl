package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Represents a function call to a VADL built-in.
 * It holds a {@link BuiltIn} function from the {@link vadl.types.BuiltInTable}.
 *
 * @see vadl.types.BuiltInTable
 * @see AbstractFunctionCallNode
 */
public class BuiltInCall extends AbstractFunctionCallNode {

  @DataValue
  protected BuiltIn builtIn;

  public BuiltInCall(BuiltIn builtIn, NodeList<ExpressionNode> args, Type type) {
    super(args, type);
    this.builtIn = builtIn;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(builtIn);
  }

  @Override
  public Node copy() {
    return new BuiltInCall(builtIn,
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        this.type());
  }
}
