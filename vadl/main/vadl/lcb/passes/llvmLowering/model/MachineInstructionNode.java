package vadl.lcb.passes.llvmLowering.model;

import java.util.List;
import vadl.ast.Expr;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.AbstractFunctionCallNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents an {@link Instruction} in a {@link Graph}.
 */
public class MachineInstructionNode extends AbstractFunctionCallNode {
  @DataValue
  protected final Instruction instruction;

  public MachineInstructionNode(NodeList<ExpressionNode> args, Instruction instruction) {
    super(args,
        Type.dummy());
    this.instruction = instruction;
  }

  @Override
  public Node copy() {
    return new MachineInstructionNode(
        new NodeList<>(args.stream().map(x -> (ExpressionNode) x.copy()).toList()),
        instruction);
  }

  @Override
  public Node shallowCopy() {
    return new MachineInstructionNode(args, instruction);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    ((TableGenMachineInstructionVisitor) visitor).visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(instruction);
  }

  public Instruction instruction() {
    return instruction;
  }
}
