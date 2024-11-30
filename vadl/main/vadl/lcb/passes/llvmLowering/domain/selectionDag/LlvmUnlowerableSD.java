package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Indicates that SD is not lowerable.
 */
public class LlvmUnlowerableSD extends ExpressionNode {

  @Nullable
  @Input
  private Node next;

  /**
   * Constructor for normal usage. The "next" field must not be initialised because the
   * {@link Node#replaceAndDelete(Node)} creates an infinity loop.
   */
  public LlvmUnlowerableSD() {
    super(Type.dummy());
  }

  private LlvmUnlowerableSD(@Nullable Node next) {
    super(Type.dummy());
    this.next = next;
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  @Nullable
  public Node next() {
    return next;
  }

  public void setNext(Node next) {
    this.next = next;
  }

  @Override
  public Node copy() {
    if (next != null) {
      return new LlvmUnlowerableSD(next.copy());
    } else {
      return new LlvmUnlowerableSD(next);
    }
  }

  @Override
  public Node shallowCopy() {
    return new LlvmUnlowerableSD(next);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.next != null) {
      collection.add(next);
    }
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    next = visitor.applyNullable(this, next);
  }
}
