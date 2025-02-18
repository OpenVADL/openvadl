package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * The TcgBrCondImm class represents a TCG (Tiny Code Generation) conditional branch with immediate.
 * This class is used to model a conditional branch operation where the branch decision is
 * determined by comparing two arguments.
 */
public class TcgBrCond extends TcgLabelNode {

  @DataValue
  private TcgCondition condition;

  @Input
  private TcgVRefNode varArg;

  @Input
  private TcgVRefNode immArg;

  /**
   * Constructs the TCG conditional branch with immediate opcode.
   * {@code tcg_gen_brcondi}.
   *
   * @param varArg    the TCG variable operand
   * @param immArg    immediate operand
   * @param condition condition when to branch
   * @param label     label to which the branch should jump to if condition is met
   */
  public TcgBrCond(TcgVRefNode varArg, TcgVRefNode immArg, TcgCondition condition,
                   TcgLabel label) {
    super(label);
    this.condition = condition;
    this.varArg = varArg;
    this.immArg = immArg;
  }

  public TcgCondition condition() {
    return condition;
  }

  public TcgVRefNode cmpArg1() {
    return varArg;
  }

  public TcgVRefNode cmpArg2() {
    return immArg;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    var used = super.usedVars();
    used.add(varArg);
    used.add(immArg);
    return used;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_brcond_" + varArg.width() + "("
        + condition.cCode() + ", "
        + cmpArg1().varName() + ", "
        + cmpArg2().varName() + ", "
        + label().varName()
        + ");";
  }

  @Override
  public Node copy() {
    return new TcgBrCond(varArg.copy(TcgVRefNode.class), immArg.copy(TcgVRefNode.class), condition,
        label());
  }

  @Override
  public Node shallowCopy() {
    return new TcgBrCond(varArg, immArg, condition, label());
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(condition);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(varArg);
    collection.add(immArg);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    varArg = visitor.apply(this, varArg, TcgVRefNode.class);
    immArg = visitor.apply(this, immArg, TcgVRefNode.class);
  }

}
