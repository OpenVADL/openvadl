package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.dependency.SliceNode;

public class InstrEndNodeReplacement
    implements GraphVisitor.NodeApplier<InstrEndNode, InstrEndNode> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public InstrEndNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public InstrEndNode visit(InstrEndNode instrEndNode) {
    for (var arg : instrEndNode.sideEffects()) {
      visitApplicable(arg);
    }
    return instrEndNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof InstrEndNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
