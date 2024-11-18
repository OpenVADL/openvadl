package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * TcgGottoTbAbs is representing a TCG (Tiny Code Generation)
 * operation node that models an absolute jump to a target program counter (PC) value.
 * This class is used in the context of QEMU's intermediate representation (IR) generation.
 */
public class TcgGottoTbAbs extends TcgNode {

  @Input
  private ExpressionNode targetPc;

  /**
   * Constructs a TcgGottoTbAbs node for TCG IR generation.
   *
   * @param targetPc The expression node representing the target program counter (PC) value.
   */
  public TcgGottoTbAbs(ExpressionNode targetPc) {
    this.targetPc = targetPc;
  }

  public ExpressionNode targetPc() {
    return targetPc;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "gen_goto_tb_abs(ctx, " + nodeToCCode.apply(targetPc) + ");";
  }

  @Override
  public Node copy() {
    return new TcgGottoTbAbs(targetPc.copy(ExpressionNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgGottoTbAbs(targetPc);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(targetPc);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    targetPc = visitor.apply(this, targetPc, ExpressionNode.class);
  }
}
