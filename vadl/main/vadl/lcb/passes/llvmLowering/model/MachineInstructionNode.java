package vadl.lcb.passes.llvmLowering.model;

import java.util.List;
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

  @DataValue
  private List<MachineInstructionParameterLink> links;

  public MachineInstructionNode(List<MachineInstructionParameterLink> links,
                                Instruction instruction) {
    super(new NodeList<>(links.stream().map(MachineInstructionParameterLink::machine).toList()),
        Type.dummy());
    this.instruction = instruction;
    this.links = links;
  }

  @Override
  public Node copy() {
    return new MachineInstructionNode(
        links,
        instruction);
  }

  @Override
  public Node shallowCopy() {
    return new MachineInstructionNode(links, instruction);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    ((TableGenMachineInstructionVisitor) visitor).visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(instruction);
    collection.add(links);
  }

  public Instruction instruction() {
    return instruction;
  }
}
