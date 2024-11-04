package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SliceNode;

public class SelectNodeReplacement
    implements GraphVisitor.NodeApplier<SelectNode, LlvmUnlowerableSD> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public SelectNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public LlvmUnlowerableSD visit(SelectNode selectNode) {
    return new LlvmUnlowerableSD();
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof SelectNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
