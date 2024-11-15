package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTruncStore;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbWriteMemNodeReplacement
    implements GraphVisitor.NodeApplier<WriteMemNode, Node> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbWriteMemNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public Node visit(WriteMemNode writeMemNode) {
    // LLVM has a special selection dag node when the memory
    // is written and the value truncated.
    if (writeMemNode.value() instanceof TruncateNode truncateNode) {
      var node = new LlvmTruncStore(writeMemNode, truncateNode);
      writeMemNode.replaceAndDelete(node);
    } else {
      var node = new LlvmStoreSD(Objects.requireNonNull(writeMemNode.address()),
          writeMemNode.value(),
          writeMemNode.memory(),
          writeMemNode.words());
      writeMemNode.replaceAndDelete(node);
    }

    if (writeMemNode.hasAddress()) {
      visitApplicable(writeMemNode.address());
    }
    visitApplicable(writeMemNode.value());

    return writeMemNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof WriteMemNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
