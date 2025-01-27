package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

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
    for (var arg : node.arguments()) {
      visitApplicable(arg);
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
