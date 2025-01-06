package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbReturnNodeReplacement
    implements GraphVisitor.NodeApplier<ReturnNode, ReturnNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbReturnNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public ReturnNode visit(ReturnNode selectNode) {
    if (selectNode.graph() != null) {
      selectNode.graph().add(new LlvmUnlowerableSD());
    }
    return selectNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ReturnNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
