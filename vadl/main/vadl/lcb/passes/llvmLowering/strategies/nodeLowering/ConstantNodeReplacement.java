package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

public class ConstantNodeReplacement implements GraphVisitor.NodeApplier<ConstantNode, ConstantNode> {
  private final List<GraphVisitor.NodeApplier<Node, Node>> replacer;

  public ConstantNodeReplacement(List<GraphVisitor.NodeApplier<Node, Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public ConstantNode visit(ConstantNode node) {
    // Upcast it to a higher type because TableGen is not able to cast implicitly.
    var types = node.usages()
        .filter(x -> x instanceof ExpressionNode)
        .map(x -> {
          var y = (ExpressionNode) x;
          // Cast to BitsType when SIntType
          return y.type();
        })
        .filter(x -> x instanceof BitsType)
        .map(x -> (BitsType) x)
        .sorted(Comparator.comparingInt(BitsType::bitWidth))
        .toList();

    var distinctTypes = new HashSet<>(types);

    if (distinctTypes.size() > 1) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Constant must be upcasted but it has multiple candidates. "
                  + "The compiler generator considered only the first type as upcast.",
              node.sourceLocation()).build());
    } else if (distinctTypes.isEmpty()) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Constant must be upcasted but it has no candidates.",
              node.sourceLocation()).build());
      return node;
    }

    var type = types.stream().findFirst().get();
    node.setType(type);
    node.constant().setType(type);

    return node;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ConstantNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<Node, Node>> recursiveHooks() {
    return replacer;
  }
}
