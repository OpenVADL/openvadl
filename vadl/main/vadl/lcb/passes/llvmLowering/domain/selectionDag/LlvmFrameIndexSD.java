package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.DataType;
import vadl.viam.Counter;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;

/**
 * LLVM node which represents the frame index as selection dag node.
 */
public class LlvmFrameIndexSD extends ReadRegFileNode implements LlvmNodeLowerable {
  public static final String NAME = "AddrFI";

  public LlvmFrameIndexSD(ReadRegFileNode obj) {
    this(obj.registerFile(), obj.address(), obj.type(), obj.staticCounterAccess());
  }

  private LlvmFrameIndexSD(RegisterFile registerFile, ExpressionNode address, DataType type,
                           @Nullable Counter.RegisterFileCounter staticCounterAccess) {
    super(registerFile, address, type, staticCounterAccess);
  }


  @Override
  public Node copy() {
    return new LlvmFrameIndexSD(registerFile(), (ExpressionNode) address().copy(), type(),
        staticCounterAccess());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmFrameIndexSD(registerFile(), address(), type(), staticCounterAccess());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    }
    if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    }
  }

  @Override
  public String lower() {
    return NAME;
  }
}
