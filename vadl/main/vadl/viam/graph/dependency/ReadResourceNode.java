package vadl.viam.graph.dependency;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.Resource;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * The ReadNode class is an abstract class that extends ExpressionNode
 * and represents a node that reads a value from an address.
 * It provides a common structure and behavior for reading nodes.
 */
public abstract class ReadResourceNode extends ExpressionNode {

  @Input
  @Nullable
  protected ExpressionNode address;

  public ReadResourceNode(@Nullable ExpressionNode address, DataType type) {
    super(type);
    this.address = address;
  }

  public ExpressionNode address() {
    ensureNonNull(address, "Address is not set. Check hasAddress before access.");
    return address;
  }

  public boolean hasAddress() {
    return address != null;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  public abstract Resource resourceDefinition();

  @Override
  public void verifyState() {
    super.verifyState();
    var resource = resourceDefinition();

    ensure(resource.hasAddress() == hasAddress(),
        "Resource takes address but this node has no address node.");

    if (address != null) {
      var addressType = address.type();
      var resAddrType = resource.addressType();
      Objects.requireNonNull(resAddrType); // just to satisfy errorprone
      ensure(addressType instanceof DataType,
          "Address must be a DataValue, was %s", address.type());
      ensure(((DataType) addressType).isTrivialCastTo(resAddrType),
          "Address value cannot be trivially cast to resource's address type. %s vs %s",
          resource.addressType(), addressType);
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

  /**
   * Checks whether the {@code address} of the node is constant and therefore statically knonw.
   */
  public boolean hasConstantAddress() {
    if (hasAddress()) {
      ensureNonNull(address, "address must not be null");
      return address.isConstant();
    }

    return false;
  }
}
