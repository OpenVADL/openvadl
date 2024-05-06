package vadl.viam.graph.dependency;

import vadl.types.DummyType;
import vadl.types.Type;

/**
 * The ReadMemNode class is a concrete class that extends ReadNode.
 * It represents a node that reads a value from a memory location.
 */
public class ReadMemNode extends ReadNode {
  public ReadMemNode(ExpressionNode location, Type type) {
    super(location, type);
  }

//  @Override
//  public Type type() {
//    return DummyType.INSTANCE;
//  }
}
