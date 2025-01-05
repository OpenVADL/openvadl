package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter;

import java.util.Objects;

/**
 * Parameter with a type and a name of a {@link TableGenParameter}.
 */
public class TableGenParameterTypeAndName extends TableGenParameter {
  private final String type;
  private final String name;

  public TableGenParameterTypeAndName(String type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public String render() {
    return String.format("%s:$%s", type, name);
  }

  public TableGenParameterTypeAndName withType(String type) {
    return new TableGenParameterTypeAndName(type, name);
  }

  public String name() {
    return name;
  }

  public String type() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o instanceof TableGenParameterTypeAndName casted) {
      return type.equals(casted.type) && name.equals(casted.name);
    }

    return false;
  }


  @Override
  public int hashCode() {
    int result = Objects.hashCode(type);
    result = 31 * result + Objects.hashCode(name);
    return result;
  }
}
