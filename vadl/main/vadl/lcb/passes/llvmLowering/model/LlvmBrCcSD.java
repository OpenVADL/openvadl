package vadl.lcb.passes.llvmLowering.model;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.visitors.MachineInstructionLcbVisitor;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM node for conditional branch.
 */
public class LlvmBrCcSD extends ExpressionNode implements LlvmNodeLowerable {

  @DataValue
  private LlvmCondCode condition;

  @Input
  private ExpressionNode first;

  @Input
  private ExpressionNode second;

  @Input
  private ExpressionNode immOffset;

  /**
   * Constructor for the LLVM node "brcc".
   */
  public LlvmBrCcSD(LlvmCondCode condition,
                    ExpressionNode first,
                    ExpressionNode second,
                    ExpressionNode immOffset) {
    super(Type.dummy());
    this.condition = condition;
    this.first = first;
    this.second = second;
    this.immOffset = immOffset;
  }

  @Override
  public String lower() {
    return "brcc";
  }

  @Override
  public Node copy() {
    return new LlvmBrCcSD(condition,
        (ExpressionNode) first.copy(),
        (ExpressionNode) second.copy(),
        (ExpressionNode) immOffset.copy());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmBrCcSD(condition, first, second, immOffset);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof MachineInstructionLcbVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  public LlvmCondCode condition() {
    return condition;
  }

  public ExpressionNode first() {
    return first;
  }

  public ExpressionNode second() {
    return second;
  }

  public ExpressionNode immOffset() {
    return immOffset;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(first);
    collection.add(second);
    collection.add(immOffset);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(condition);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    first = visitor.apply(this, first, ExpressionNode.class);
    second = visitor.apply(this, second, ExpressionNode.class);
    immOffset = visitor.apply(this, immOffset, ExpressionNode.class);
  }
}
