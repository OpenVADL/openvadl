package vadl.lcb.passes.llvmLowering.model;

import java.util.Objects;
import vadl.lcb.passes.llvmLowering.LlvmMayLoadMemory;
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
public class LlvmStore extends WriteMemNode implements LlvmNodeLowerable,
    LlvmSideEffectPatternIncluded, LlvmMayLoadMemory {

  public LlvmStore(ExpressionNode address,
                   ExpressionNode value,
                   Memory memory,
                   int words) {
    super(memory, words, address, value);
  }

  @Override
  public Node copy() {
    return new LlvmStore((ExpressionNode) Objects.requireNonNull(address).copy(),
        (ExpressionNode) value.copy(),
        memory,
        words);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmStore(Objects.requireNonNull(address), value, memory, words);
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
