package vadl.lcb.passes.llvmLowering.domain.machineDag;

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Nodes in the machine graph for {@link TableGenPattern} which indicates that the instruction
 * is wrapping other arguments.
 */
public class LcbMachineInstructionWrappedNode extends ExpressionNode {

  @DataValue
  protected Instruction instruction;
  @Input
  protected NodeList<ExpressionNode> args;

  /**
   * Constructor.
   */
  public LcbMachineInstructionWrappedNode(Instruction instruction, NodeList<ExpressionNode> args) {
    super(Type.dummy());
    this.instruction = instruction;
    this.args = args;
  }

  public Instruction instruction() {
    return instruction;
  }

  public NodeList<ExpressionNode> arguments() {
    return args;
  }

  @Override
  public Node copy() {
    return new LcbMachineInstructionWrappedNode(instruction,
        new NodeList<>(this.args.stream().map(x -> (ExpressionNode) x.copy()).toList())
    );
  }

  @Override
  public Node shallowCopy() {
    return new LcbMachineInstructionWrappedNode(instruction, this.args);
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

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(args);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    args = args.stream()
        .map((e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }
}
