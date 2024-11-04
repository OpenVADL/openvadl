package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.types.BuiltInTable;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.WriteRegNode;

public class WriteRegNodeReplacement
    implements GraphVisitor.NodeApplier<WriteRegNode, WriteRegNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public WriteRegNodeReplacement(
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
        var conditional = getConditional(Objects.requireNonNull(writeRegNode.graph()));
        var condCond = LlvmCondCode.from(conditional.builtIn());
        if (condCond == null) {
          throw new ViamError("CondCode must be not null");
        }

        var first = conditional.arguments().get(0);
        var second = conditional.arguments().get(1);
        var immOffset =
            builtin.arguments().stream().filter(x -> x instanceof LlvmBasicBlockSD)
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

  private LlvmSetccSD getConditional(Graph behavior) {
    var builtIn = behavior.getNodes(LlvmSetccSD.class)
        .filter(
            x -> Set.of(BuiltInTable.EQU, BuiltInTable.NEQ, BuiltInTable.SLTH, BuiltInTable.ULTH,
                    BuiltInTable.SGEQ, BuiltInTable.UGEQ, BuiltInTable.SLEQ, BuiltInTable.ULEQ)
                .contains(x.builtIn()))
        .findFirst();

    if (builtIn.isEmpty()) {
      throw new ViamError(
          "Visitor wrongly used. Are you sure this is a conditional branch instruction?");
    }

    return builtIn.get();
  }
}
