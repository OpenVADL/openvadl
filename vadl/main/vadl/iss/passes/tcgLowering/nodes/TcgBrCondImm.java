package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The TcgBrCondImm class represents a TCG (Tiny Code Generation) conditional branch with immediate.
 * This class is used to model a conditional branch operation where the branch decision is
 * determined by comparing two arguments.
 */
public class TcgBrCondImm extends TcgLabelNode {

  @DataValue
  private TcgCondition condition;

  @DataValue
  private TcgV varArg;

  @Input
  private ExpressionNode immArg;

  /**
   * Constructs the TCG conditional branch with immediate opcode.
   * {@code tcg_gen_brcondi}.
   *
   * @param varArg    the TCG variable operand
   * @param immArg    immediate operand
   * @param condition condition when to branch
   * @param label     label to which the branch should jump to if condition is met
   */
  public TcgBrCondImm(TcgV varArg, ExpressionNode immArg, TcgCondition condition,
                      TcgLabel label) {
    super(label);
    this.condition = condition;
    this.varArg = varArg;
    this.immArg = immArg;
  }

  public TcgCondition condition() {
    return condition;
  }

  public TcgV cmpArg1() {
    return varArg;
  }

  public ExpressionNode cmpArg2() {
    return immArg;
  }

  @Override
  public Node copy() {
    return new TcgBrCondImm(varArg, immArg.copy(ExpressionNode.class), condition, label());
  }

  @Override
  public Node shallowCopy() {
    return new TcgBrCondImm(varArg, immArg, condition, label());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(condition);
    collection.add(varArg);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(immArg);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    immArg = visitor.apply(this, immArg, ExpressionNode.class);
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_brcondi_" + varArg.width() + "("
        + condition.cCode() + ", "
        + cmpArg1().varName() + ", "
        + nodeToCCode.apply(cmpArg2()) + ", "
        + label().varName()
        + ");";
  }
}
