package vadl.lcb.passes.llvmLowering.domain.machineDag;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Nodes in the machine graph for {@link TableGenPattern}.
 */
public class MachineInstructionParameterNode extends ExpressionNode {
  @DataValue
  private TableGenInstructionOperand instructionOperand;

  public MachineInstructionParameterNode(TableGenInstructionOperand instructionOperand) {
    super(Type.dummy());
    this.instructionOperand = instructionOperand;
  }

  public TableGenInstructionOperand instructionOperand() {
    return instructionOperand;
  }

  public void setInstructionOperand(TableGenInstructionOperand operand) {
    this.instructionOperand = operand;
  }

  @Override
  public Node copy() {
    return new MachineInstructionParameterNode(instructionOperand);
  }

  @Override
  public Node shallowCopy() {
    return new MachineInstructionParameterNode(instructionOperand);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    ((TableGenMachineInstructionVisitor) visitor).visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(instructionOperand);
  }
}
