package vadl.cppCodeGen.model;


import java.util.List;
import vadl.cppCodeGen.CppCodeGenGraphNodeVisitor;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * This node indicates that a value should be updated.
 * Note that the semantic of this node is that it returns not only the updated value but the
 * application of the slice .
 */
public class CppUpdateBitRangeNode extends ExpressionNode {
  @Input
  public ExpressionNode value;
  @Input
  public ExpressionNode patch;
  @DataValue
  public Format.Field field;

  public CppUpdateBitRangeNode(Type type,
                               ExpressionNode value,
                               ExpressionNode patch,
                               Format.Field field) {
    super(type);
    this.value = value;
    this.patch = patch;
    this.field = field;
  }

  @Override
  public Node copy() {
    return new CppUpdateBitRangeNode(this.type(), (ExpressionNode) value.copy(),
        (ExpressionNode) patch.copy(), field);
  }

  @Override
  public Node shallowCopy() {
    return new CppUpdateBitRangeNode(this.type(), value, patch, field);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    ((CppCodeGenGraphNodeVisitor) visitor).visit(this);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
    collection.add(patch);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(field);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
    patch = visitor.apply(this, patch, ExpressionNode.class);
  }
}
