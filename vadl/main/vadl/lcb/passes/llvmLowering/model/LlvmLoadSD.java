package vadl.lcb.passes.llvmLowering.model;

import java.util.Objects;
import vadl.lcb.passes.llvmLowering.LlvmMayLoadMemory;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.LlvmSideEffectPatternIncluded;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.DataType;
import vadl.viam.Memory;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadMemNode;

/**
 * LLVM node for a memory load.
 */
public class LlvmLoadSD extends ReadMemNode implements LlvmNodeLowerable,
    LlvmSideEffectPatternIncluded, LlvmMayLoadMemory {

  public LlvmLoadSD(ReadMemNode readMemNode) {
    this(readMemNode.address(), readMemNode.memory(), readMemNode.words());
  }

  public LlvmLoadSD(ExpressionNode address,
                    Memory memory,
                    int words) {
    super(memory, words, address, (DataType) address.type());
  }

  @Override
  public Node copy() {
    return new LlvmLoadSD((ExpressionNode) Objects.requireNonNull(address).copy(),
        memory,
        words);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmLoadSD(Objects.requireNonNull(address), memory, words);
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
    return "load";
  }
}
