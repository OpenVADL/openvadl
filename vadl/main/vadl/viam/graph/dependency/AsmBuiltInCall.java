package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents a call to a built-in function of the asm parser.
 * It holds a {@link BuiltIn} from the {@link vadl.types.BuiltInTable#ASM_PARSER_BUILT_INS}.
 *
 * @see vadl.types.BuiltInTable
 * @see AbstractFunctionCallNode
 */
public class AsmBuiltInCall extends AbstractFunctionCallNode {

  @DataValue
  protected BuiltIn asmBuiltIn;

  public AsmBuiltInCall(BuiltIn asmBuiltIn, NodeList<ExpressionNode> args, Type type) {
    super(args, type);
    this.asmBuiltIn = asmBuiltIn;
  }

  /**
   * Update the asm builtin by the given value.
   */
  public void setAsmBuiltIn(BuiltIn asmBuiltIn) {
    this.asmBuiltIn = asmBuiltIn;
  }

  /**
   * Gets the asm {@link BuiltIn}.
   */
  public BuiltIn asmBuiltIn() {
    return this.asmBuiltIn;
  }

  @Override
  public ExpressionNode copy() {
    return new AsmBuiltInCall(asmBuiltIn,
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        this.type());
  }

  @Override
  public Node shallowCopy() {
    return new AsmBuiltInCall(asmBuiltIn, args, type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(asmBuiltIn);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
