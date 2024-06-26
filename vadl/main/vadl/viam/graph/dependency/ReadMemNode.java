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
   * Number of words read from memory.
   */
  @DataValue
  protected int words;

  /**
   * Constructs a ReadMemNode object with the specified memory, address, and data type.
   *
   * @param memory  the memory definition from which to read the value
   * @param words   the number of words that are read from address ({@code MEM<words>(addr)})
   * @param address the address expression node representing the address in memory to read from
   * @param type    the data type of the value being read
   */
  public ReadMemNode(Memory memory, int words, ExpressionNode address, DataType type) {
    super(address, type);
    this.memory = memory;
    this.words = words;

    verifyState();
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(memory.wordSize() * words == type().bitWidth(),
        "Type missmatch of expected node type and read bit width. %s vs %s",
        type().bitWidth(), memory.wordSize() * words);
  }

  @Override
  protected Resource resourceDefinition() {
    return memory;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(memory);
    collection.add(words);
  }
}
