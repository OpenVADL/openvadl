package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.InstrEndNode;

public class BranchEndNodeReplacement
    implements GraphVisitor.NodeApplier<BranchEndNode, BranchEndNode> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public BranchEndNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public BranchEndNode visit(BranchEndNode branchEndNode) {
    for (var arg : branchEndNode.sideEffects()) {
      visitApplicable(arg);
    }
    return branchEndNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof BranchEndNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
