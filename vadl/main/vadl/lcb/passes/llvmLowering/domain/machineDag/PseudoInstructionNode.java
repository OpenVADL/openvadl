package vadl.lcb.passes.llvmLowering.domain.machineDag;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.types.Type;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.AbstractFunctionCallNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents an {@link PseudoInstruction} in a {@link Graph}.
 */
public class PseudoInstructionNode extends AbstractFunctionCallNode {
  @DataValue
  protected final PseudoInstruction instruction;

  /**
   * Constructor.
   */
  public PseudoInstructionNode(NodeList<ExpressionNode> args,
                               PseudoInstruction instruction) {
    super(args,
        Type.dummy());
    this.instruction = instruction;
  }


  @Override
  public Node copy() {
    return new PseudoInstructionNode(
        new NodeList<>(args.stream().map(x -> (ExpressionNode) x.copy()).toList()),
        instruction);
  }

  @Override
  public Node shallowCopy() {
    return new PseudoInstructionNode(args, instruction);
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

  public PseudoInstruction instruction() {
    return instruction;
  }
}
