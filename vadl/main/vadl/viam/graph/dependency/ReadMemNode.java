package vadl.viam.graph.dependency;

import org.jetbrains.annotations.Nullable;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * The ReadMemNode class is a concrete class that extends ReadNode.
 * It represents a node that reads a value from a memory location.
 */
public class ReadMemNode extends ReadNode {

  public ReadMemNode(ExpressionNode address, DataType type) {
    super(address, type);
  }

}
