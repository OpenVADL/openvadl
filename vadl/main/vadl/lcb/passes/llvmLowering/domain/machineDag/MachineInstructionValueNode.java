package vadl.lcb.passes.llvmLowering.domain.machineDag;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Nodes in the machine graph for {@link TableGenPattern}. This is special because
 * it represents only a value.
 */
public class MachineInstructionValueNode extends ExpressionNode {
  @DataValue
  private ValueType valueType;
  @DataValue
  private Constant constant;

  public MachineInstructionValueNode(ValueType valueType, Constant constant) {
    super(Type.dummy());
    this.valueType = valueType;
    this.constant = constant;
  }

  public ValueType valueType() {
    return valueType;
  }

  public Constant constant() {
    return constant;
  }

  @Override
  public Node copy() {
    return new MachineInstructionValueNode(valueType, constant);
  }

  @Override
  public Node shallowCopy() {
    return new MachineInstructionValueNode(valueType, constant);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    ((TableGenMachineInstructionVisitor) visitor).visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(valueType);
    collection.add(constant);
  }
}
