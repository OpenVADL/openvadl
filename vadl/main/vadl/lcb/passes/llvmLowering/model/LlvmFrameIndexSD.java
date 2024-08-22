package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.DataType;
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
    this(obj.registerFile(), obj.address(), obj.type());
  }

  private LlvmFrameIndexSD(RegisterFile registerFile, ExpressionNode address, DataType type) {
    super(registerFile, address, type);
  }


  @Override
  public Node copy() {
    return new LlvmFrameIndexSD(registerFile(), (ExpressionNode) address().copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmFrameIndexSD(registerFile(), address(), type());
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
