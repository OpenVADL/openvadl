package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.SelectNode;

public class ReturnNodeReplacement
    implements GraphVisitor.NodeApplier<ReturnNode, LlvmUnlowerableSD> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public ReturnNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public LlvmUnlowerableSD visit(ReturnNode selectNode) {
    return new LlvmUnlowerableSD();
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ReturnNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
