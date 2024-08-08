package vadl.cpp_codegen.passes.type_normalization;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.cpp_codegen.OopGraphNodeVisitor;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TypeCastNode;

/**
 * VADL and CPP have not the same types. VADL supports arbitrary bit sizes whereas CPP does not.
 * The {@link CppTypeNormalizer} converts these types, however, we want to keep the original
 * type information. This class extends the {@link TypeCastNode}. So the {@link TypeCastNode}
 * contains the upcasted type and this {@link UpcastedTypeCastNode} has a member for the
 * {@code originalType}.
 */
public class UpcastedTypeCastNode extends TypeCastNode {
  @DataValue
  private final Type originalType;

  public UpcastedTypeCastNode(ExpressionNode value, Type upcastedType, Type originalType) {
    super(value, upcastedType);
    this.originalType = originalType;
  }

  /**
   * Get the type before the typecast.
   */
  public Type originalType() {
    return this.originalType;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(originalType);
  }

  @Override
  public Node copy() {
    return new UpcastedTypeCastNode((ExpressionNode) value.copy(), type(), originalType);
  }

  @Override
  public Node shallowCopy() {
    return new UpcastedTypeCastNode(value, type(), originalType);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    accept((OopGraphNodeVisitor) visitor);
  }

  public void accept(OopGraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
