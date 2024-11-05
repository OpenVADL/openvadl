package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SelectNode;

/**
 * Replacement strategy for nodes.
 */
public class LlvmUnlowerableNodeReplacement
    implements GraphVisitor.NodeApplier<LlvmUnlowerableSD, LlvmUnlowerableSD> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LlvmUnlowerableNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public LlvmUnlowerableSD visit(LlvmUnlowerableSD node) {
    if (node.next() != null) {
      visitApplicable(node.next());
    }
    return node;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof LlvmUnlowerableSD;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
