package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.dump.Info;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.ReturnNode;

public class IfNodeReplacement
    implements GraphVisitor.NodeApplier<IfNode, LlvmUnlowerableSD> {
  @Nullable
  @Override
  public LlvmUnlowerableSD visit(IfNode selectNode) {
    return new LlvmUnlowerableSD();
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof IfNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return List.of();
  }
}
