package vadl.iss.passes.tcgLowering.nodes;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * A TCG node that represents a generic helper call.
 */
public class TcgHelperCall extends TcgNode {

  @Input
  @Nullable
  TcgVRefNode result;

  @Input
  NodeList<DependencyNode> args;

  // if true, the tcg_env is passed as the first argument to the helper
  @DataValue
  boolean envArg;
  @DataValue
  String name;

  /**
   * Constructs the helper call node.
   */
  public TcgHelperCall(@Nullable TcgVRefNode result, NodeList<DependencyNode> args, boolean envArg,
                       String name) {
    this.result = result;
    this.args = args;
    this.envArg = envArg;
    this.name = name;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    var argNames = this.args.stream().map(a -> {
      if (a instanceof TcgVRefNode tcgV) {
        return tcgV.cCode();
      } else if (a instanceof ExpressionNode expr) {
        // TODO: don't hardcode this constant
        var tcgWidth = Tcg_32_64.fromWidth(expr.type().asDataType().bitWidth());
        return "tcg_constant_" + tcgWidth.name() + "(" + nodeToCCode.apply(expr) + ")";
      } else {
        throw new ViamGraphError("Unexpected node type: %s", a)
            .addContext(this);
      }
    });
    var resultName = this.result != null ? this.result.cCode() : null;
    var envArg = this.envArg ? "tcg_env" : null;
    var args = Streams.concat(Stream.of(resultName), Stream.of(envArg), argNames)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(", "));
    return "gen_helper_" + name + "(" + args + ");";
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return args.stream()
        .filter(s -> s instanceof TcgVRefNode)
        .map(s -> (TcgVRefNode) s)
        .collect(Collectors.toSet());
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    if (result == null) {
      return List.of();
    }
    return List.of(result);
  }

  @Override
  public Node copy() {
    return new TcgHelperCall(result != null ? result.copy() : null, args.copy(), envArg, name);
  }

  @Override
  public Node shallowCopy() {
    return new TcgHelperCall(result, args, envArg, name);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.result != null) {
      collection.add(result);
    }
    collection.addAll(args);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(envArg);
    collection.add(name);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    result = visitor.applyNullable(this, result, TcgVRefNode.class);
    args = args.stream().map((e) -> visitor.apply(this, e, DependencyNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }
}
