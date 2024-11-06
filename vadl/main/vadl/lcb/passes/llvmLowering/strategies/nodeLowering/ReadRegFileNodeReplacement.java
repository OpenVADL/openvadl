package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadRegFileNode;

/**
 * Replacement strategy for nodes.
 */
public class ReadRegFileNodeReplacement
    implements GraphVisitor.NodeApplier<ReadRegFileNode, Node> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public ReadRegFileNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public Node visit(ReadRegFileNode readRegFileNode) {
    // If the address is constant and register file has a constraint for, then we should replace it
    // by the constraint value.

    if (readRegFileNode.hasConstantAddress()) {
      var address = (ConstantNode) readRegFileNode.address();
      var constraint = Arrays.stream(readRegFileNode.registerFile().constraints())
          .filter(c -> c.address().equals(address.constant()))
          .findFirst();

      if (constraint.isPresent()) {
        return new ConstantNode(constraint.get().value());
      } else {
        DeferredDiagnosticStore.add(Diagnostic.warning(
            "Reading from a register file with constant index but the register has no "
                + "constraint value.",
            address.sourceLocation()).build());
        return readRegFileNode;
      }
    } else {
      visitApplicable(readRegFileNode.address());

      return new LlvmReadRegFileNode(readRegFileNode.registerFile(), readRegFileNode.address(),
          readRegFileNode.type(), readRegFileNode.staticCounterAccess());
    }
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ReadRegFileNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
