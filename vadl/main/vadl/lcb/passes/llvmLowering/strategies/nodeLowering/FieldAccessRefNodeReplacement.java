package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.SliceNode;

public class FieldAccessRefNodeReplacement
    implements GraphVisitor.NodeApplier<FieldAccessRefNode, LlvmFieldAccessRefNode> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;
  private final ValueType architectureType;

  public FieldAccessRefNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer,
                                       ValueType architectureType) {
    this.replacer = replacer;
    this.architectureType = architectureType;
  }

  @Nullable
  @Override
  public LlvmFieldAccessRefNode visit(FieldAccessRefNode fieldAccessRefNode) {
    var originalType = fieldAccessRefNode.fieldAccess().accessFunction().returnType();

    return
        new LlvmFieldAccessRefNode(fieldAccessRefNode.fieldAccess(),
            originalType,
            architectureType);
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof FieldAccessRefNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
