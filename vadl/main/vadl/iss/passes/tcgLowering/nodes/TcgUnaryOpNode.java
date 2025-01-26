package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents an abstract unary operation node within the Tiny Code Generator (TCG) framework.
 * This class serves as a base for specific unary operations by handling
 * common functionality such as argument management and data collection.
 */
public abstract class TcgUnaryOpNode extends TcgOpNode {

  @Input
  TcgVRefNode arg;

  public TcgUnaryOpNode(TcgVRefNode dest, TcgVRefNode arg) {
    super(dest, dest.width());
    this.arg = arg;
  }

  public TcgVRefNode arg() {
    return arg;
  }

  public abstract String tcgFunctionName();

  @Override
  public Set<TcgVRefNode> usedVars() {
    var sup = super.usedVars();
    sup.add(arg);
    return sup;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + firstDest().varName() + ", " + arg.varName() + ");";
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(arg);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arg = visitor.apply(this, arg, TcgVRefNode.class);
  }
}
