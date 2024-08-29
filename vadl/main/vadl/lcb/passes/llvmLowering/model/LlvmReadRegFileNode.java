package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.ParameterIdentity;
import vadl.types.DataType;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;

/**
 * LLVM node for the selection dag.
 */
public class LlvmReadRegFileNode extends ReadRegFileNode implements LlvmNodeLowerable,
    LlvmNodeReplaceable {
  private final ParameterIdentity parameterIdentity;

  /**
   * Constructor.
   */
  public LlvmReadRegFileNode(RegisterFile registerFile,
                             ExpressionNode address,
                             DataType type) {
    super(registerFile, address, type);
    ensure(address instanceof FieldRefNode, "address must be a field");
    this.parameterIdentity = ParameterIdentity.from(this, (FieldRefNode) address);
  }

  @Override
  public ParameterIdentity parameterIdentity() {
    return parameterIdentity;
  }

  @Override
  public Node copy() {
    return new LlvmReadRegFileNode(registerFile, (ExpressionNode) address().copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmReadRegFileNode(registerFile, address(), type());
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
