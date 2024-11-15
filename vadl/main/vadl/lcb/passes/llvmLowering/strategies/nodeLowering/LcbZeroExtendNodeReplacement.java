package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmZExtLoad;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbZeroExtendNodeReplacement
    implements GraphVisitor.NodeApplier<ZeroExtendNode, Node> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbZeroExtendNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public Node visit(ZeroExtendNode node) {
    if (node.value() instanceof ReadMemNode readMemNode) {
      // Merge SignExtend and ReadMem to LlvmZExtLoad
      node.replaceAndDelete(
          new LlvmTypeCastSD(new LlvmZExtLoad(readMemNode), makeSigned(node.type())));
      visitApplicable(readMemNode.address());
    } else {
      // Remove all nodes
      for (var usage : node.usages().toList()) {
        usage.replaceInput(node, node.value());
      }
      visitApplicable(node.value());
    }
    return node;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ZeroExtendNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }

  private Type makeSigned(DataType type) {
    if (!type.isSigned()) {
      if (type instanceof BitsType bitsType) {
        return SIntType.bits(bitsType.bitWidth());
      }
    }

    return type;
  }
}
