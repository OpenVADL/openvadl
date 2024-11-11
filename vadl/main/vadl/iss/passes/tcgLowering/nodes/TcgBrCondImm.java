package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
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
public class TcgBrCondImm extends TcgOpNode {

  @DataValue
  private TcgCondition condition;

  @DataValue
  private TcgLabel label;

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
    // TODO: This super constructor is useless. We need to create a TcgGenNode super type
    super(TcgV.gen(TcgWidth.i64), TcgWidth.i64);
    this.condition = condition;
    this.label = label;
    this.varArg = varArg;
    this.immArg = immArg;
  }

  public TcgCondition condition() {
    return condition;
  }

  public TcgLabel label() {
    return label;
  }

  public TcgV cmpArg1() {
    return varArg;
  }

  public ExpressionNode cmpArg2() {
    return immArg;
  }

  @Override
  public Node copy() {
    return new TcgBrCondImm(varArg, immArg.copy(ExpressionNode.class), condition, label);
  }

  @Override
  public Node shallowCopy() {
    return new TcgBrCondImm(varArg, immArg, condition, label);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(condition);
    collection.add(label);
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
}
