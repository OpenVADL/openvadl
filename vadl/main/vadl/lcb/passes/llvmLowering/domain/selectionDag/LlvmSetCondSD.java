package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Generic llvm selection dag node for conditionals.
 */
public class LlvmSetCondSD extends BuiltInCall implements LlvmNodeLowerable {
  @DataValue
  private final LlvmCondCode llvmCondCode;

  public LlvmSetCondSD(BuiltInTable.BuiltIn builtIn,
                       NodeList<ExpressionNode> args,
                       Type type) {
    super(builtIn, args, type);
    this.llvmCondCode = LlvmCondCode.from(builtIn);
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

  @Override
  public ExpressionNode copy() {
    return new LlvmSetCondSD(builtIn,
        new NodeList<>(args.stream().map(x -> (ExpressionNode) x.copy()).toList()),
        type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmSetCondSD(builtIn, args, type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(llvmCondCode);
  }

  @Override
  public String lower() {
    return llvmCondCode.name().toLowerCase();
  }
}
