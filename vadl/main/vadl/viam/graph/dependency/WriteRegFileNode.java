package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.graph.UniqueNode;

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

  public WriteRegFileNode(RegisterFile registerFile, ExpressionNode location,
                          ExpressionNode value) {
    super(location, value);
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
}
