package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;

public class TruncateNodeReplacement
    implements GraphVisitor.NodeApplier<TruncateNode, TruncateNode> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public TruncateNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public TruncateNode visit(TruncateNode truncateNode) {
    // Remove all nodes
    for (var usage : truncateNode.usages().toList()) {
      usage.replaceInput(truncateNode, truncateNode.value());
    }
    visitApplicable(truncateNode.value());
    return truncateNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof TruncateNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
