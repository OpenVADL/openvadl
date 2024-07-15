package vadl.viam.graph.dependency;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.oop.SymbolTable;
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
  private ExpressionNode address;

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

  protected abstract Resource resourceDefinition();

  @Override
  public void verifyState() {
    super.verifyState();
    var resource = resourceDefinition();

    ensure(resource.resultType().canBeCastTo(type()),
        "Mismatching resource type. Resource's result type (%s) "
        + "cannot be cast to node's type (%s).",
        resource.resultType(), type());

    ensure(resource.hasAddress() == hasAddress(),
        "Resource takes address but this node has no address node.");

    if (address != null) {
      var addressType = address.type();
      var resAddrType = resource.addressType();
      Objects.requireNonNull(resAddrType); // just to satisfy errorprone
      ensure(addressType instanceof DataType,
          "Address must be a DataValue, was %s", address.type());
      ensure(((DataType) addressType).canBeCastTo(resAddrType),
          "Address value cannot be cast to resource's address type. %s vs %s",
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

  @Override
  public String generateOopExpression(SymbolTable symbolTable) {
    throw new RuntimeException("not implemented");
  }
}
