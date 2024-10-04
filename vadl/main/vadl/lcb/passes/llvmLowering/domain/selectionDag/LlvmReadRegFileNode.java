package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;
import vadl.types.DataType;
import vadl.viam.Counter;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;

/**
 * LLVM node for the selection dag.
 */
public class LlvmReadRegFileNode extends ReadRegFileNode implements LlvmNodeLowerable,
    LlvmNodeReplaceable {
  protected ParameterIdentity parameterIdentity;

  /**
   * Constructor.
   */
  public LlvmReadRegFileNode(RegisterFile registerFile,
                             ExpressionNode address,
                             DataType type,
                             @Nullable Counter.RegisterFileCounter staticCounterAccess) {
    super(registerFile, address, type, staticCounterAccess);
    parameterIdentity = ParameterIdentity.from(this, address);
  }

  @Override
  public ParameterIdentity parameterIdentity() {
    return parameterIdentity;
  }

  @Override
  public Node copy() {
    return new LlvmReadRegFileNode(registerFile, (ExpressionNode) address().copy(), type(),
        staticCounterAccess());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmReadRegFileNode(registerFile, address(), type(), staticCounterAccess());
  }

  @Override
  public String lower() {
    return parameterIdentity.render();
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
}
