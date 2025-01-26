package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * A common superclass that represents a TCG operator with two source variables and one result.
 */
public abstract class TcgBinaryOpNode extends TcgOpNode {

  @Input
  TcgVRefNode arg1;

  @Input
  TcgVRefNode arg2;

  public TcgBinaryOpNode(TcgVRefNode dest, TcgVRefNode arg1, TcgVRefNode arg2) {
    this(dest, arg1, arg2, dest.width());
  }

  /**
   * Constructs a TcgBinaryOpNode with specified result variable, two argument variables,
   * and a specified bit width.
   *
   * @param resultVar the variable that will store the result of the binary operation
   * @param arg1      the first argument variable
   * @param arg2      the second argument variable
   * @param width     the bit width of the operation
   */
  public TcgBinaryOpNode(TcgVRefNode resultVar, TcgVRefNode arg1, TcgVRefNode arg2,
                         Tcg_32_64 width) {
    super(resultVar, width);
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  public TcgBinaryOpNode(NodeList<TcgVRefNode> destinations, TcgVRefNode arg1, TcgVRefNode arg2,
                         Tcg_32_64 width) {
    super(destinations, width);
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(arg1.var().width() == width(), "argument 1 width does not match");
    ensure(arg2.var().width() == width(), "argument 2 width does not match");
  }

  public TcgVRefNode arg1() {
    return arg1;
  }

  public TcgVRefNode arg2() {
    return arg2;
  }

  public abstract String tcgFunctionName();

  @Override
  public Set<TcgVRefNode> usedVars() {
    var sup = super.usedVars();
    sup.addAll(Set.of(arg1, arg2));
    return sup;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "_" + width() + "("
        + firstDest().cCode() + ", "
        + arg1.cCode() + ", "
        + arg2.cCode()
        + ");";
  }


  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(arg1);
    collection.add(arg2);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arg1 = visitor.apply(this, arg1, TcgVRefNode.class);
    arg2 = visitor.apply(this, arg2, TcgVRefNode.class);
  }
}
