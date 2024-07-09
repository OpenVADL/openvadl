package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.UniqueNode;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Represents a write to register file.
 *
 * <p>Even though this is a side effect, it is both, a {@link DependencyNode}
 * and a {@link UniqueNode}. This is because of VADL's semantic constraints
 * for register writes:
 * <li>A register may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteRegFileNode extends WriteResourceNode {

  @DataValue
  protected RegisterFile registerFile;

  /**
   * Writes a value to a register file node.
   *
   * @param registerFile The register file to write to.
   * @param address      The index/address node of the register file.
   * @param value        The value to be written.
   */
  public WriteRegFileNode(RegisterFile registerFile, ExpressionNode address,
                          ExpressionNode value) {
    super(address, value);
    this.registerFile = registerFile;

    verifyState();
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  @Override
  protected Resource resourceDefinition() {
    return registerFile;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(registerFile);
  }

  @Override
  public Node copy() {
    return new WriteRegFileNode(registerFile,
        address != null ? (ExpressionNode) address.copy() : null, (ExpressionNode) value.copy());
  }

  @Override
  public Node shallowCopy() {
    return new WriteRegFileNode(registerFile, null, null);
  }
}
