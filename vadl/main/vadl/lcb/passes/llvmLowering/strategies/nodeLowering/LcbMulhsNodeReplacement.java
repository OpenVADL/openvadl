package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSMulhSD;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Replacement strategy for nodes which are do signed multiplication and then slice the upper part.
 */
public class LcbMulhsNodeReplacement
    implements GraphVisitor.NodeApplier<SliceNode, BuiltInCall> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;
  private final Set<BuiltInTable.BuiltIn> builtins =
      Set.of(BuiltInTable.SMULL, BuiltInTable.SMULLS);

  public LcbMulhsNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public BuiltInCall visit(SliceNode x) {
    var node = (BuiltInCall) x.value();
    for (var arg : node.arguments()) {
      visitApplicable(arg);
    }

    return node.replaceAndDelete(new LlvmSMulhSD(node.arguments(), node.type()));
  }

  @Override
  public boolean acceptable(Node node) {
    if (node instanceof SliceNode sd && sd.value() instanceof BuiltInCall bc
        && builtins.contains(bc.builtIn())) {
      /*
        Example: `ty` is `int128`
        then `high` is `128` and `64`.
        A SliceNode requires the bounds `lsb` = `64` and `msb` = `127`.
       */
      var ty = (BitsType) bc.type();
      var high = ty.bitWidth();
      var low = high / 2;
      return sd.bitSlice().lsb() == low
          && sd.bitSlice().msb() == high - 1;
    }

    return false;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
