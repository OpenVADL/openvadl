package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SliceNode;

public class SliceNodeReplacement
    implements GraphVisitor.NodeApplier<SliceNode, SliceNode> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public SliceNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public SliceNode visit(SliceNode sliceNode) {
    return sliceNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof SliceNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
