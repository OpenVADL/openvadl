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
 * LLVM node for a memory load zero extend.
 */
public class LlvmZExtLoad extends ReadMemNode implements LlvmNodeLowerable,
    LlvmSideEffectPatternIncluded, LlvmMayLoadMemory {

  public LlvmZExtLoad(ReadMemNode readMemNode) {
    this(readMemNode.address(), readMemNode.memory(), readMemNode.words());
  }

  public LlvmZExtLoad(ExpressionNode address,
                      Memory memory,
                      int words) {
    super(memory, words, address, (DataType) address.type());
  }

  @Override
  public Node copy() {
    return new LlvmZExtLoad((ExpressionNode) Objects.requireNonNull(address).copy(),
        memory,
        words);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmZExtLoad(Objects.requireNonNull(address), memory, words);
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
    ensure(memory.wordSize() == 8, "Memory word size must be 8 because "
        + "LLVM requires it");
    return "zextload" + words * memory.wordSize();
  }
}
