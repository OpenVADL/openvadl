package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DummyType;

/**
 * The ReadRegNode class is a subclass of ReadNode that represents
 * a node that reads a value from a register location.
 */
public class ReadRegNode extends ReadNode {

  @DataValue
  protected String register; // TODO: Replace by proper datastructure

  public ReadRegNode(String register, ExpressionNode location) {
    super(location, DummyType.INSTANCE);
    this.register = register;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(register);
  }
}
