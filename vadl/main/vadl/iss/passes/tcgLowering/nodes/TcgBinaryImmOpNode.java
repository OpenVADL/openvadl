package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * A common superclass that represents a TCG operator with one source variable, one
 * immediate, and one result.
 */
public abstract class TcgBinaryImmOpNode extends TcgOpNode {

  @DataValue
  TcgV arg1;

  @Input
  ExpressionNode arg2;

  /**
   * Constructs the tcg binary imm op.
   *
   * @param res   the result variable
   * @param arg1  the first argument variable
   * @param arg2  the second argument immediate
   * @param width the op's width
   */
  public TcgBinaryImmOpNode(TcgV res, TcgV arg1, ExpressionNode arg2, TcgWidth width) {
    super(res, width);
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(arg1.width() == width, "argument 1 width does not match. %s vs %s", arg1.width(), width);
    ensure(arg2.type().isData(), "argument 2 is not a data type");
    ensure(
        Objects.requireNonNull(((DataType) arg2.type()).fittingCppType()).bitWidth() <= width.width,
        "argument 2 width does not match. %s vs %s", arg1.width(), arg2.type());
  }

  public TcgV arg1() {
    return arg1;
  }

  public ExpressionNode arg2() {
    return arg2;
  }

  public abstract String tcgFunctionName();

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "_" + width + "("
        + dest.varName() + ", "
        + arg1.varName() + ", "
        + nodeToCCode.apply(arg2)
        + ");";
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(arg1);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(arg2);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arg2 = visitor.apply(this, arg2, ExpressionNode.class);
  }
}
