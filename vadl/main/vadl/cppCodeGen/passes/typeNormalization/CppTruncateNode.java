package vadl.cppCodeGen.passes.typeNormalization;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * VADL and CPP have not the same types. VADL supports arbitrary bit sizes whereas CPP does not.
 * The {@link CppTypeNormalizationPass} converts these types, however, we want to keep the original
 * type information. This class extends the {@link TruncateNode}. So the {@link TruncateNode}
 * contains the upcasted type and this {@link CppTruncateNode} has a member for the
 * {@code originalType}.
 */
public class CppTruncateNode extends TruncateNode {
  @DataValue
  private final Type originalType;

  public CppTruncateNode(ExpressionNode value, DataType type, Type originalType) {
    super(value, type);
    this.originalType = originalType;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(originalType);
  }
}
