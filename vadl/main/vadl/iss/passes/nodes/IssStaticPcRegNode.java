package vadl.iss.passes.nodes;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Register;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a static program counter (PC) register in an expression tree.
 * In the TCG generation context, we can get the current PC from {@code ctx->pc_curr},
 * so a read to the PC cpu register (TCGv) is not necessary.
 */
public class IssStaticPcRegNode extends ExpressionNode {

  @DataValue
  private final Register pc;

  public IssStaticPcRegNode(Register pc) {
    super(pc.resultType());
    this.pc = pc;
  }

  public Register register() {
    return pc;
  }

  @Override
  public ExpressionNode copy() {
    return new IssStaticPcRegNode(pc);
  }

  @Override
  public Node shallowCopy() {
    return new IssStaticPcRegNode(pc);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(pc);
  }
}
