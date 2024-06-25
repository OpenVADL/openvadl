package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.graph.UniqueNode;

/**
 * Represents a write to memory.
 *
 * <p>Even though this is a side effect, it is both, a {@link DependencyNode}
 * and a {@link UniqueNode}. This is because of VADL's semantic constraints
 * for memory writes:
 * <li>A location may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteMemNode extends WriteResourceNode {

  @DataValue
  protected Memory memory;

  /**
   * Constructs a new WriteMemNode object.
   *
   * @param memory  the memory definition to write to
   * @param address the expression representing the memory address
   * @param value   the expression representing the value to write
   */
  public WriteMemNode(Memory memory, ExpressionNode address, ExpressionNode value) {
    super(address, value);
    this.memory = memory;

    verifyState();
  }

  public Memory memory() {
    return memory;
  }

  @Override
  protected Resource resourceDefinition() {
    return memory;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(memory);
  }
}
