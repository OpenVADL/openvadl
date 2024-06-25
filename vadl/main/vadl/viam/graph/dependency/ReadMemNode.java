package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.Memory;
import vadl.viam.Resource;

/**
 * The ReadMemNode class is a concrete class that extends ReadNode.
 * It represents a node that reads a value from a memory location.
 */
public class ReadMemNode extends ReadResourceNode {

  @DataValue
  protected Memory memory;
  
  /**
   * Constructs a ReadMemNode object with the specified memory, address, and data type.
   *
   * @param memory  the memory definition from which to read the value
   * @param address the address expression node representing the address in memory to read from
   * @param type    the data type of the value being read
   */
  public ReadMemNode(Memory memory, ExpressionNode address, DataType type) {
    super(address, type);
    this.memory = memory;

    verifyState();
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
