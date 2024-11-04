package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.WriteRegFileNode;

public class WriteRegFileNodeReplacement
    implements GraphVisitor.NodeApplier<WriteRegFileNode, WriteRegFileNode> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public WriteRegFileNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
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
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
