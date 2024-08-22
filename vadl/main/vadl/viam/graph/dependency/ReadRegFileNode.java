package vadl.viam.graph.dependency;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * A read of a register file in the behaviour graph. It takes one expression node as input
 * that represents the address/index value.
 */
public class ReadRegFileNode extends ReadResourceNode {

  @DataValue
  protected RegisterFile registerFile;

  public ReadRegFileNode(RegisterFile registerFile, ExpressionNode address,
                         DataType type) {
    super(address, type);
    this.registerFile = registerFile;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  @Override
  protected Resource resourceDefinition() {
    return registerFile;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(registerFile.resultType().isTrivialCastTo(type()),
        "Mismatching register file type. Register file's result type (%s) "
            + "cannot be trivially cast to node's type (%s).",
        registerFile.resultType(), type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(registerFile);
  }

  @Override
  public Node copy() {
    return new ReadRegFileNode(registerFile, (ExpressionNode) address().copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new ReadRegFileNode(registerFile, address(), type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
