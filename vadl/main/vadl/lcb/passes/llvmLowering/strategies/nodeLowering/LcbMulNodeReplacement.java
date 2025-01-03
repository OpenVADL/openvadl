package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSMulhSD;
import vadl.types.BuiltInTable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * Replacement strategy for nodes which are do multiplication and then truncate to the lower part.
 */
public class LcbMulNodeReplacement
    implements GraphVisitor.NodeApplier<BuiltInCall, BuiltInCall> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  /*
    `MUL` and `SMUL` need to be covered in the normal BuiltinReplacement.
    The reason why we are using `BuiltInTable.SMULL, BuiltInTable.SMULLS` is that the "normal"
    multiplication requires two nodes: arithmetic and slice / truncate node.
   */
  private final Set<BuiltInTable.BuiltIn> builtins =
      Set.of(BuiltInTable.SMULL, BuiltInTable.SMULLS);

  public LcbMulNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public BuiltInCall visit(BuiltInCall node) {
    for (var arg : node.arguments()) {
      visitApplicable(arg);
    }

    return node.replaceAndDelete(new LlvmSMulhSD(node.arguments(), node.type()));
  }

  @Override
  public boolean acceptable(Node node) {
    if (node instanceof BuiltInCall bc && builtins.contains(bc.builtIn())) {
      // There are two approaches:
      // (1) Cut the result
      // (2) Cut the inputs
      return
          bc.usages().allMatch(usage -> usage instanceof TruncateNode)
              || bc.arguments().stream().allMatch(arg -> arg instanceof TruncateNode);
    }

    return false;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
