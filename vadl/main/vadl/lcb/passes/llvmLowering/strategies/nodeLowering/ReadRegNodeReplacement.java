package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadRegNode;

/**
 * Replacement strategy for nodes.
 */
public class ReadRegNodeReplacement implements GraphVisitor.NodeApplier<ReadRegNode, ReadRegNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public ReadRegNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public ReadRegNode visit(ReadRegNode node) {
    if (node.hasAddress()) {
      visitApplicable(node.address());
    }

    return node;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ReadRegNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
