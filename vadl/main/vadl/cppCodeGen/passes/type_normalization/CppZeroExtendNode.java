package vadl.cppCodeGen.passes.type_normalization;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ZeroExtendNode;


/**
 * VADL and CPP have not the same types. VADL supports arbitrary bit sizes whereas CPP does not.
 * The {@link CppTypeNormalizer} converts these types, however, we want to keep the original
 * type information. This class extends the {@link ZeroExtendNode}. So the {@link ZeroExtendNode}
 * contains the upcasted type and this {@link CppZeroExtendNode} has a member for the
 * {@code originalType}.
 */
public class CppZeroExtendNode extends ZeroExtendNode {
  @DataValue
  private final Type originalType;

  public CppZeroExtendNode(ExpressionNode value,
                           DataType type,
                           Type originalType) {
    super(value, type);
    this.originalType = originalType;
  }

  public Type originalType() {
    return originalType;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(originalType);
  }
}
