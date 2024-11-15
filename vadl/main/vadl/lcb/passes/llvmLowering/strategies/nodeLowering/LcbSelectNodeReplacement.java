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
public class LcbSelectNodeReplacement
    implements GraphVisitor.NodeApplier<SelectNode, SelectNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbSelectNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public SelectNode visit(SelectNode selectNode) {
    visitApplicable(selectNode.condition());
    visitApplicable(selectNode.trueCase());
    visitApplicable(selectNode.falseCase());
    if (selectNode.graph() != null) {
      selectNode.graph().add(new LlvmUnlowerableSD());
    }
    return selectNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof SelectNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
