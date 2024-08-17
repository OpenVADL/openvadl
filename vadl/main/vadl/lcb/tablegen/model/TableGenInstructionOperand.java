package vadl.lcb.tablegen.model;

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
}
