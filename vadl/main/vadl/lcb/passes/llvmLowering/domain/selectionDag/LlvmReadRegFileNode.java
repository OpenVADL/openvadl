package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionIndexedRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionRegisterFileOperand;
import vadl.types.DataType;
import vadl.viam.Counter;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;

/**
 * LLVM node for the selection dag.
 */
public class LlvmReadRegFileNode extends ReadRegFileNode implements LlvmNodeLowerable,
    LlvmNodeReplaceable {
  protected TableGenInstructionOperand instructionOperand;

  /**
   * Constructor.
   */
  public LlvmReadRegFileNode(RegisterFile registerFile,
                             FieldRefNode address,
                             DataType type,
                             @Nullable Counter.RegisterFileCounter staticCounterAccess) {
    super(registerFile, address, type, staticCounterAccess);
    instructionOperand = new TableGenInstructionRegisterFileOperand(this, address);
  }

  /**
   * Constructor.
   */
  public LlvmReadRegFileNode(RegisterFile registerFile,
                             ExpressionNode address,
                             DataType type,
                             @Nullable Counter.RegisterFileCounter staticCounterAccess) {
    super(registerFile, address, type, staticCounterAccess);
    if (address instanceof FieldRefNode fieldRefNode) {
      instructionOperand = new TableGenInstructionRegisterFileOperand(this, fieldRefNode);
    } else if (address instanceof FuncParamNode funcParamNode) {
      instructionOperand = new TableGenInstructionIndexedRegisterFileOperand(this, funcParamNode);
    } else {
      throw Diagnostic.error("Not supported", address.sourceLocation()).build();
    }
  }

  @Override
  public TableGenInstructionOperand operand() {
    return instructionOperand;
  }

  @Override
  public ExpressionNode copy() {
    return new LlvmReadRegFileNode(registerFile, (ExpressionNode) address().copy(), type(),
        staticCounterAccess());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmReadRegFileNode(registerFile, address(), type(), staticCounterAccess());
  }

  @Override
  public String lower() {
    return operand().render();
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
