package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbSliceNodeReplacement
    implements GraphVisitor.NodeApplier<SliceNode, SliceNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbSliceNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
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
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
