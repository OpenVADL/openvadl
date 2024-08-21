package vadl.lcb.passes.llvmLowering.model;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.BitsType;
import vadl.types.Type;
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * This is a special LLVM node which is a combination of
 * {@link WriteMemNode} and {@link vadl.viam.graph.dependency.TruncateNode}.
 */
public class LlvmTruncStore extends WriteResourceNode implements LlvmNodeLowerable {

  @DataValue
  protected Memory memory;

  @DataValue
  protected int words;

  @DataValue
  protected Type truncatedType;

  public LlvmTruncStore(@Nullable ExpressionNode address,
                        TruncateNode value,
                        Memory memory,
                        int words) {
    super(address, value);
    this.memory = memory;
    this.words = words;
    this.truncatedType = value.type();
  }

  @Override
  public Node copy() {
    return new LlvmTruncStore((ExpressionNode) Objects.requireNonNull(address).copy(),
        (TruncateNode) value.copy(),
        memory,
        words);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmTruncStore(address, (TruncateNode) value, memory, words);
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
    collection.add(truncatedType);
  }

  @Override
  public String lower() {
    return "truncstorei" + words * 8;
  }
}
