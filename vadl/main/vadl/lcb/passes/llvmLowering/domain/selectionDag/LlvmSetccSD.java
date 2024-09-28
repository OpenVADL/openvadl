package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.List;
import java.util.Set;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.AbstractFunctionCallNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM Node for logical comparison.
 */
public class LlvmSetccSD extends AbstractFunctionCallNode implements LlvmNodeLowerable {
  @DataValue
  protected BuiltInTable.BuiltIn builtIn;

  public static Set<BuiltInTable.BuiltIn> supported = Set.of(
      BuiltInTable.EQU,
      BuiltInTable.NEQ,
      BuiltInTable.SGTH,
      BuiltInTable.UGTH,
      BuiltInTable.SLTH,
      BuiltInTable.ULTH,
      BuiltInTable.SLEQ,
      BuiltInTable.ULEQ,
      BuiltInTable.SGEQ,
      BuiltInTable.UGEQ
  );

  private LlvmCondCode llvmCondCode;

  /**
   * Constructor for LlvmSetccSD.
   */
  public LlvmSetccSD(BuiltInTable.BuiltIn built,
                     NodeList<ExpressionNode> args,
                     Type type) {
    super(args, type);
    this.builtIn = built;
    var condCode = LlvmCondCode.from(built);
    if (condCode != null) {
      llvmCondCode = condCode;
    } else {
      throw new ViamError("not supported cond code");
    }
  }

  @Override
  public String lower() {
    return "setcc";
  }

  public LlvmCondCode llvmCondCode() {
    return llvmCondCode;
  }

  /**
   * Gets the {@link BuiltInTable.BuiltIn}.
   */
  public BuiltInTable.BuiltIn builtIn() {
    return this.builtIn;
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
  public Node copy() {
    return new LlvmSetccSD(builtIn,
        new NodeList<>(args.stream().map(x -> (ExpressionNode) x.copy()).toList()),
        type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmSetccSD(builtIn, args, type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(builtIn);
  }
}
