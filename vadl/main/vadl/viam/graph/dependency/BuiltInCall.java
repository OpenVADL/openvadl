package vadl.viam.graph.dependency;

import java.util.List;
import java.util.Optional;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents a function call to a VADL built-in.
 * It holds a {@link BuiltIn} function from the {@link vadl.types.BuiltInTable}.
 *
 * @see vadl.types.BuiltInTable
 * @see AbstractFunctionCallNode
 */
public class BuiltInCall extends AbstractFunctionCallNode implements Canonicalizable {

  @DataValue
  protected BuiltIn builtIn;

  public BuiltInCall(BuiltIn builtIn, NodeList<ExpressionNode> args, Type type) {
    super(args, type);
    this.builtIn = builtIn;
  }

  /**
   * Update the builtin by the given value.
   */
  public void setBuiltIn(BuiltIn builtIn) {
    this.builtIn = builtIn;
  }

  /**
   * Gets the {@link BuiltIn}.
   */
  public BuiltIn builtIn() {
    return this.builtIn;
  }


  @Override
  public Optional<Node> normalize() {
    if (!hasConstantArgs()) {
      return Optional.empty();
    }

    var args = this.arguments().stream()
        .map(x -> ((ConstantNode) x).constant())
        .toList();

    return builtIn.compute(args).map(ConstantNode::new);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void canonicalize() {
    arguments().forEach(ExpressionNode::canonicalize);

    if (isCommutative()) {
      // Sort arguments s.t constants are last
      // and when multiple constants then highest last
      this.arguments().sort((o1, o2) -> {
        if (o1 instanceof ConstantNode && !(o2 instanceof ConstantNode)) {
          return 1;
        } else if (!(o1 instanceof ConstantNode) && o2 instanceof ConstantNode) {
          return -1;
        } else if (o1 instanceof ConstantNode) { // && o2 instanceof ConstantNode
          var c1 = (ConstantNode) o1;             // is statically known
          var c2 = (ConstantNode) o2;

          if (c1.constant() instanceof Constant.Value
              && c2.constant() instanceof Constant.Value) {
            return ((Constant.Value) c1.constant()).integer()
                .compareTo(((Constant.Value) c2.constant()).integer());
          }
        }

        return 0;
      });
    }
  }


  @Override
  public Node canonical() {
    if (hasConstantArgs()) {
      var args = this.arguments().stream()
          .map(x -> ((ConstantNode) x).constant())
          .toList();

      return builtIn
          .compute(args)
          .map(e -> (Node) new ConstantNode(e))
          .orElse(this);
    }

    if (args.size() == 2) {
      // binary operation

      if (isCommutative() && args.get(0) instanceof ConstantNode) {
        // place constant node on the right side
        var copy = (BuiltInCall) copy();
        //noinspection ComparatorMethodParameterNotUsed
        copy.arguments().sort((a, b) -> 1);
        return copy;
      }

    }

    return this;
  }

  @Override
  public boolean isCommutative() {
    return BuiltInTable.commutative.contains(this.builtIn);
  }

  @Override
  public Node copy() {
    return new BuiltInCall(builtIn,
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        this.type());
  }

  @Override
  public Node shallowCopy() {
    return new BuiltInCall(builtIn, args, type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(builtIn);
  }
}
