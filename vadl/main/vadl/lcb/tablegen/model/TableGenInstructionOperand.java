package vadl.lcb.tablegen.model;

import java.util.Objects;

/**
 * An {@link TableGenInstruction} has list of operands for inputs and outputs.
 * This class represent one element of the inputs or outputs.
 */
public class TableGenInstructionOperand {
  private final String type;
  private final String name;

  public TableGenInstructionOperand(String type, String name) {
    this.type = type;
    this.name = name;
  }

  public String render() {
    return String.format("%s:$%s", type, name);
  }


  public String type() {
    return type;
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TableGenInstructionOperand x) {
      return x.type.equals(this.type) && x.name.equals(this.name);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "TableGenInstructionOperand{"
        + "type='" + type + '\''
        + ", name='" + name + '\''
        + '}';
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name);
  }
}
