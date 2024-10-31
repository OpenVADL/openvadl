package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM node for unconditional jump.
 */
public class LlvmBrSD extends ExpressionNode implements LlvmNodeLowerable {

  @Input
  private ExpressionNode bb;

  /**
   * Constructor for the LLVM node "brcc".
   */
  public LlvmBrSD(ExpressionNode bb) {
    super(Type.dummy());
    this.bb = bb;
  }

  @Override
  public String lower() {
    return "br";
  }

  @Override
  public Node copy() {
    return new LlvmBrSD(
        (ExpressionNode) bb.copy());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmBrSD(bb);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    }
  }

  public ExpressionNode bb() {
    return bb;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(bb);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    bb = visitor.apply(this, bb, ExpressionNode.class);
  }
}
