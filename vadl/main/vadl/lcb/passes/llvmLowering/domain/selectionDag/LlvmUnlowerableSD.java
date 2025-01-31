package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.List;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Indicates that SD is not lowerable.
 */
public class LlvmUnlowerableSD extends BuiltInCall {

  public LlvmUnlowerableSD(NodeList<ExpressionNode> args,
                           Type type) {
    super(BuiltInTable.ADD, args, type);
  }

  /**
   * Constructor for normal usage. The "next" field must not be initialised because the
   * {@link Node#replaceAndDelete(Node)} creates an infinity loop.
   */
  public LlvmUnlowerableSD() {
    super(BuiltInTable.ADD, new NodeList<>(), Type.dummy());
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

  @Override
  public ExpressionNode copy() {
    return new LlvmUnlowerableSD(
        new NodeList<>(args.stream().map(ExpressionNode::copy).toList()),
        type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmUnlowerableSD(args, type());
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
  }
}
