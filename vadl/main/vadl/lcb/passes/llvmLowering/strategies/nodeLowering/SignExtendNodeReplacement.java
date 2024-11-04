package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.SignExtendNode;

public class SignExtendNodeReplacement
    implements GraphVisitor.NodeApplier<SignExtendNode, Node> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public SignExtendNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public Node visit(SignExtendNode node) {
    if (node.value() instanceof ReadMemNode readMemNode) {
      // Merge SignExtend and ReadMem to LlvmSExtLoad
      node.replaceAndDelete(
          new LlvmTypeCastSD(new LlvmSExtLoad(readMemNode), makeSigned(node.type())));
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
    return node instanceof SignExtendNode;
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
