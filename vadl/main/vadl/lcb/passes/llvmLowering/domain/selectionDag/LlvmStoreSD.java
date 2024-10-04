package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.Objects;
import vadl.lcb.passes.llvmLowering.LlvmMayStoreMemory;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.LlvmSideEffectPatternIncluded;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.viam.Memory;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * LLVM node for a memory store.
 */
public class LlvmStoreSD extends WriteMemNode implements LlvmNodeLowerable,
    LlvmSideEffectPatternIncluded, LlvmMayStoreMemory {

  public LlvmStoreSD(ExpressionNode address,
                     ExpressionNode value,
                     Memory memory,
                     int words) {
    super(memory, words, address, value);
  }

  @Override
  public Node copy() {
    return new LlvmStoreSD((ExpressionNode) Objects.requireNonNull(address).copy(),
        (ExpressionNode) value.copy(),
        memory,
        words);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmStoreSD(Objects.requireNonNull(address), value, memory, words);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  @Override
  public String lower() {
    return "store";
  }
}
