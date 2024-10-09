package vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity;

import java.util.Objects;

/**
 * Parameter identity with a type and a name.
 */
public class ParameterTypeAndNameIdentity extends ParameterIdentity {
  private final String type;
  private final String name;

  public ParameterTypeAndNameIdentity(String type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public String render() {
    return String.format("%s:$%s", type, name);
  }

  public ParameterTypeAndNameIdentity withType(String type) {
    return new ParameterTypeAndNameIdentity(type, name);
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

    if (o instanceof ParameterTypeAndNameIdentity casted) {
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
