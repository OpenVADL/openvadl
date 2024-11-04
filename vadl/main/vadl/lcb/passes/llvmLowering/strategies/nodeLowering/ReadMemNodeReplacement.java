package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmLoadSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegNode;

public class ReadMemNodeReplacement
    implements GraphVisitor.NodeApplier<ReadMemNode, LlvmTypeCastSD> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public ReadMemNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public LlvmTypeCastSD visit(ReadMemNode readMemNode) {
    visitApplicable(readMemNode.address());
    return new LlvmTypeCastSD(new LlvmLoadSD(readMemNode), readMemNode.type());
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ReadMemNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
