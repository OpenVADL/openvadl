package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameter;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.graph.Node;

/**
 * An {@link TableGenInstruction} has list of operands for inputs and outputs.
 * This class represent one element of the inputs or outputs.
 */
public class TableGenInstructionOperand {

  @Nullable
  protected final Node origin;

  private final TableGenParameter parameter;

  public TableGenInstructionOperand(@Nullable Node origin, TableGenParameter parameter) {
    this.parameter = parameter;
    this.origin = origin;
  }

  public TableGenInstructionOperand(@Nullable Node origin, String type, String name) {
    this(origin, new TableGenParameterTypeAndName(type, name));
  }

  public String render() {
    return parameter.render();
  }

  public TableGenParameter parameter() {
    return parameter;
  }

  @Nullable
  public Node origin() {
    return origin;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TableGenInstructionOperand x) {
      return x.parameter.equals(this.parameter);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameter);
  }
}
