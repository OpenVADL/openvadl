package vadl.viam.graph.dependency;

import static java.util.Collections.reverse;

import java.util.Iterator;
import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.Type;
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
   * Utility constructor for a built-in call.
   */
  public static BuiltInCall of(BuiltIn builtIn, ExpressionNode... args) {
    return of(builtIn, List.of(args));
  }

  /**
   * Utility constructor for a built-in call.
   */
  public static BuiltInCall of(BuiltIn builtIn, List<ExpressionNode> args) {
    var argList = new NodeList<>(args);
    var type = builtIn.returns(
        args.stream()
            .map(ExpressionNode::type).toList()
    );
    return new BuiltInCall(builtIn, argList, type);
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
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Node canonical() {
    if (hasConstantArgs()) {
      // constant evaluation
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
        // place constant node on the right side of operator
        var copy = (BuiltInCall) shallowCopy();
        // from left to right -> reverse
        reverse(copy.arguments());
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
  public void verifyState() {
    super.verifyState();
    var argTypeClasses = builtIn.signature().argTypeClasses();

    ensure(argTypeClasses.size() == this.arguments().size(),
        "Number of arguments must match, %s vs %s", argTypeClasses.size(), this.arguments().size());

    var actualArgTypes = this.arguments().stream().map(ExpressionNode::type).toList();
    ensure(builtIn.takes(actualArgTypes),
        "Arguments' types do not match with the type of the builtin. Args: %s",
        actualArgTypes);

    var builtInResultType = builtIn.returns(actualArgTypes);
    ensure(builtInResultType.isTrivialCastTo(this.type()),
        "BuiltIns' result type does not match node's type. %s vs %s", builtInResultType, this.type()
    );
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

  @Override
  public void prettyPrint(StringBuilder sb) {
    if (builtIn.operator() != null && builtIn.argTypeClasses().size() == 2) {
      sb.append("(");
      args.get(0).prettyPrint(sb);
      sb.append(" ").append(builtIn.operator()).append(" ");
      args.get(1).prettyPrint(sb);
      sb.append(")");
    } else {
      sb.append(builtIn.name());
      sb.append("(");

      for (int i = 0; i < args.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        args.get(i).prettyPrint(sb);
      }

      sb.append(")");
    }
  }
}
