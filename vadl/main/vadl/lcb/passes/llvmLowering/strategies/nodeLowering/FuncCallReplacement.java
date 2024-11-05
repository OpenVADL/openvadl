package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FuncCallNode;

/**
 * Replacement strategy for nodes.
 */
public class FuncCallReplacement
    implements GraphVisitor.NodeApplier<FuncCallNode, FuncCallNode> {
  @Nullable
  @Override
  public FuncCallNode visit(FuncCallNode selectNode) {
    if (selectNode.graph() != null) {
      selectNode.graph().add(new LlvmUnlowerableSD());
    }
    return selectNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof FuncCallNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return List.of();
  }
}
