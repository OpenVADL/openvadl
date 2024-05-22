package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Format;

/**
 * Represents a node that holds a reference to a format immediate.
 */
public class ImmediateRefNode extends ParamNode {

  @DataValue
  protected Format.Immediate immediate;

  /**
   * Creates an ImmediateRefNode object that holds a reference to a format immediate.
   *
   * @param immediate the format immediate to be referenced
   */
  public ImmediateRefNode(Format.Immediate immediate) {
    super(immediate.type());

    this.immediate = immediate;
  }

  public Format.Immediate immediate() {
    return immediate;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(immediate);
  }
}
