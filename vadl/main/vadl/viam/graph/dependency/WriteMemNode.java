package vadl.viam.graph.dependency;

import java.util.List;
import java.util.Objects;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
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

  @DataValue
  protected int words;

  /**
   * Constructs a new WriteMemNode object.
   *
   * @param memory  the memory definition to write to
   * @param words   the number of words that are written to memory
   * @param address the expression representing the memory address
   * @param value   the expression representing the value to write
   */
  public WriteMemNode(Memory memory, int words, ExpressionNode address, ExpressionNode value) {
    super(address, value);
    this.memory = memory;
    this.words = words;

    verifyState();
  }

  public Memory memory() {
    return memory;
  }

  public int words() {
    return words;
  }

  @Override
  protected Resource resourceDefinition() {
    return memory;
  }

  @Override
  protected int writeBitWidth() {
    return memory.wordSize() * words;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(memory);
    collection.add(words);
  }

  @Override
  public Node copy() {
    return new WriteMemNode(memory, words,
        (ExpressionNode) Objects.requireNonNull(address).copy(),
        (ExpressionNode) value.copy());
  }

  @Override
  public Node shallowCopy() {
    return new WriteMemNode(memory, words, Objects.requireNonNull(address), value);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
