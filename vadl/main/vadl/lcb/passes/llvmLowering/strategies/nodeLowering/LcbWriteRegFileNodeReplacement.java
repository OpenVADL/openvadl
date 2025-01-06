package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import javax.annotation.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbWriteRegFileNodeReplacement
    implements GraphVisitor.NodeApplier<WriteRegFileNode, WriteRegFileNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbWriteRegFileNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public WriteRegFileNode visit(WriteRegFileNode node) {
    if (node.hasAddress()) {
      visitApplicable(node.address());
    }
    visitApplicable(node.value());

    return node;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof WriteRegFileNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
