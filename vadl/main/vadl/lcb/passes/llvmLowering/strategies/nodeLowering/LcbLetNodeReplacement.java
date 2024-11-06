package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbLetNodeReplacement implements GraphVisitor.NodeApplier<LetNode, ExpressionNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbLetNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public ExpressionNode visit(LetNode node) {
    visitApplicable(node.expression());
    return node.expression();
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof LetNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
