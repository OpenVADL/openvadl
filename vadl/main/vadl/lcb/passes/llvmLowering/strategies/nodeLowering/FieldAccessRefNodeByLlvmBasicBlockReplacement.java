package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * While {@link FieldAccessRefNodeReplacement} converts every
 * {@link FieldAccessRefNode} into {@link LlvmFieldAccessRefNode},
 * this class converts it into {@link LlvmBasicBlockSD}. This means that the field should be
 * treated like an immediate, but it is a basic block.
 */
public class FieldAccessRefNodeByLlvmBasicBlockReplacement
    implements GraphVisitor.NodeApplier<FieldAccessRefNode, LlvmFieldAccessRefNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;
  private final ValueType architectureType;

  public FieldAccessRefNodeByLlvmBasicBlockReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer,
      ValueType architectureType) {
    this.replacer = replacer;
    this.architectureType = architectureType;
  }

  @Nullable
  @Override
  public LlvmFieldAccessRefNode visit(FieldAccessRefNode fieldAccessRefNode) {
    var originalType = fieldAccessRefNode.fieldAccess().accessFunction().returnType();

    return
        new LlvmFieldAccessRefNode(fieldAccessRefNode.fieldAccess(),
            originalType,
            architectureType);
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof FieldAccessRefNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
