package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.UnaryNode;

/**
 * LLVM's node for type casts.
 */
public class LlvmTypeCastSD extends UnaryNode implements LlvmNodeLowerable {
  private ValueType valueType;

  public LlvmTypeCastSD(ExpressionNode value, Type type) {
    super(value, type);
    this.valueType = ValueType.from(type);
  }

  @Override
  public Node copy() {
    return new LlvmTypeCastSD((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmTypeCastSD(value, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    ((TableGenNodeVisitor) visitor).visit(this);
  }

  @Override
  public String lower() {
    return valueType.getLlvmType();
  }
}
