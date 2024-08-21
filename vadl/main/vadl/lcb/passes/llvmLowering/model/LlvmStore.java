package vadl.lcb.passes.llvmLowering.model;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.passes.llvmLowering.LlvmMayLoadMemory;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.LlvmSideEffectPatternIncluded;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.Type;
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * LLVM node for a memory store.
 */
public class LlvmStore extends WriteResourceNode implements LlvmNodeLowerable,
    LlvmSideEffectPatternIncluded, LlvmMayLoadMemory {

  @DataValue
  protected Memory memory;

  @DataValue
  protected int words;

  public LlvmStore(@Nullable ExpressionNode address,
                   ExpressionNode value,
                   Memory memory,
                   int words) {
    super(address, value);
    this.memory = memory;
    this.words = words;
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
    return new LlvmStore(address, value, memory, words);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  @Override
  protected Resource resourceDefinition() {
    return memory;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(memory);
    collection.add(words);
  }

  @Override
  public String lower() {
    return "store";
  }
}
