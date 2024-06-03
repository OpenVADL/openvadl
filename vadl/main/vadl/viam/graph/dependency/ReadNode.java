package vadl.viam.graph.dependency;

import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * The ReadNode class is an abstract class that extends ExpressionNode
 * and represents a node that reads a value from an address.
 * It provides a common structure and behavior for reading nodes.
 */
public abstract class ReadNode extends ExpressionNode {

  @Input
  @Nullable
  private ExpressionNode address;

  public ReadNode(@Nullable ExpressionNode address, DataType type) {
    super(type);
    this.address = address;
  }

  public ExpressionNode address() {
    ensureNonNull(address, "Location is not set");
    return address;
  }

  public boolean hasAddress() {
    return address != null;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public void verifyState() {
    super.verifyState();
    if (address != null) {
      ensure(address.type() instanceof DataType,
          "Address must be a DataValue, was %s", address.type());
    }
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.address != null) {
      collection.add(address);
    }
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    address = visitor.applyNullable(this, address, ExpressionNode.class);
  }
}
