package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.types.BuiltInTable;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbWriteRegNodeReplacement
    implements GraphVisitor.NodeApplier<WriteRegNode, WriteRegNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbWriteRegNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public WriteRegNode visit(WriteRegNode writeRegNode) {
    if (writeRegNode.hasAddress()) {
      visitApplicable(writeRegNode.address());
    }

    visitApplicable(writeRegNode.value());

    // this will get the nullable static counter access
    // if the reg write node writes the pc, this will not be null
    var pc = writeRegNode.staticCounterAccess();
    if (pc != null) {
      if (writeRegNode.value() instanceof BuiltInCall builtin && Set.of(
          BuiltInTable.ADD,
          BuiltInTable.ADDS,
          BuiltInTable.SUB
      ).contains(builtin.builtIn())) {
        // We need to four parameters.
        // 1. the conditional code (SETEQ, ...)
        // 2. the first operand of the comparison
        // 3. the second operand of the comparison
        // 4. the immediate offset

        // idea: it would be good to have a link from the side effect to if-node.
        var conditional = (BuiltInCall) writeRegNode.condition();
        var condCond = LlvmCondCode.from(conditional.builtIn());
        if (condCond == null) {
          throw new ViamError("CondCode must be not null");
        }

        var first = conditional.arguments().get(0);
        var second = conditional.arguments().get(1);
        var immOffset =
            builtin.arguments().stream().filter(x -> x instanceof FieldAccessRefNode)
                .findFirst();

        if (immOffset.isEmpty()) {
          throw new ViamError("Immediate Offset is missing");
        }

        writeRegNode.value().replaceAndDelete(new LlvmBrCcSD(
            condCond,
            first,
            second,
            immOffset.get()
        ));
      }
    }

    return writeRegNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof WriteRegNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
