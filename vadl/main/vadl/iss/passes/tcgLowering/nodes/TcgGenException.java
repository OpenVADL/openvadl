package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * A TCG node that represents the {@code generate_exception} function to raise an exception
 * in TCG.
 * This substitutes the {@code raise} statement in VADL.
 */
public class TcgGenException extends TcgNode {

  @DataValue
  int exceptionCode;

  public TcgGenException(int exceptionCode) {
    this.exceptionCode = exceptionCode;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "generate_exception(ctx, " + exceptionCode + ")";
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return Set.of();
  }

  @Override
  public @Nullable TcgVRefNode definedVar() {
    return null;
  }

  @Override
  public Node copy() {
    return new TcgGenException(exceptionCode);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(exceptionCode);
  }
}
