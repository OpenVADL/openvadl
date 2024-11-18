package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * A common superclass that represents a TCG operator with one source variable, one
 * immediate, and one result.
 */
public abstract class TcgUnaryImmOpNode extends TcgOpNode {

  @Input
  ExpressionNode arg;

  /**
   * Constructs the tcg binary imm op.
   *
   * @param res the result variable
   * @param arg the immediate argument
   */
  public TcgUnaryImmOpNode(TcgV res, ExpressionNode arg) {
    super(res, res.width());
    this.arg = arg;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(arg.type().isData(), "argument 2 is not a data type");
    ensure(
        Objects.requireNonNull(((DataType) arg.type()).fittingCppType()).bitWidth() <= width.width,
        "argument 2 width does not match. %s vs %s", dest.width(), arg.type());
  }

  public ExpressionNode arg() {
    return arg;
  }

  public abstract String tcgFunctionName();

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + dest.varName() + ", " + nodeToCCode.apply(arg) + ");";
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(arg);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arg = visitor.apply(this, arg, ExpressionNode.class);
  }
}
