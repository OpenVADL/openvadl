package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;
import vadl.viam.graph.Node;

/**
 * An {@link TableGenInstruction} has list of operands for inputs and outputs.
 * This class represent one element of the inputs or outputs.
 */
public class TableGenInstructionOperand {

  protected final Node origin;

  private ParameterIdentity identity;

  public TableGenInstructionOperand(Node origin, ParameterIdentity identity) {
    this.identity = identity;
    this.origin = origin;
  }

  public TableGenInstructionOperand(Node origin, String type, String name) {
    this(origin, new ParameterIdentity(type, name));
  }

  public String render() {
    return identity.render();
  }

  public ParameterIdentity identity() {
    return identity;
  }

  public void updateIdentity(ParameterIdentity identity) {
    this.identity = identity;
  }

  public Node origin() {
    return origin;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TableGenInstructionOperand x) {
      return x.identity.equals(this.identity);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(identity);
  }
}
