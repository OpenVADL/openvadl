package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;

public class InstrCallNodeReplacement
    implements GraphVisitor.NodeApplier<InstrCallNode, InstrCallNode> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public InstrCallNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
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
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
