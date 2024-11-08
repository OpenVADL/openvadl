package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.Function;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * A call to a function in a behaviour graph.
 *
 * <p>It has a list of arguments that must match the expected parameter by the given
 * {@link Function} definition.</p>
 */
public class FuncCallNode extends AbstractFunctionCallNode {

  @DataValue
  protected Function function;

  public FuncCallNode(NodeList<ExpressionNode> args, Function function, Type type) {
    super(args, type);
    this.function = function;
  }


  public Function function() {
    return function;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    var params = function.parameters();
    var args = this.args;

    ensure(params.length == args.size(),
        "Number of arguments does not match number of parameters, %s vs %s", args.size(),
        params.length);

    for (int i = 0; i < args.size(); i++) {
      var arg = args.get(i);
      var param = params[i];
      ensure(param.type().isTrivialCastTo(arg.type()),
          "Argument does not match type of param %s, %s vs %s", param.simpleName(), param.type(),
          arg.type());
    }
    ensure(function.returnType().isTrivialCastTo(type()),
        "Return type of function does not match declared result type %s",
        type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(function);
  }

  @Override
  public Node copy() {
    return new FuncCallNode(
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        function, type());
  }

  @Override
  public Node shallowCopy() {
    return new FuncCallNode(arguments(), function, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(function.simpleName())
        .append("(");
    for (int i = 0; i < arguments().size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      arguments().get(i).prettyPrint(sb);
    }
    sb.append(")");
  }
}
