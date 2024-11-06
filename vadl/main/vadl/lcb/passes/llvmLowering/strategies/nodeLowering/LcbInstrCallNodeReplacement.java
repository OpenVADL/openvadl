package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbInstrCallNodeReplacement
    implements GraphVisitor.NodeApplier<InstrCallNode, InstrCallNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbInstrCallNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public InstrCallNode visit(InstrCallNode instrCallNode) {
    for (var arg : instrCallNode.arguments()) {
      visitApplicable(arg);
    }
    return instrCallNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof InstrCallNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
