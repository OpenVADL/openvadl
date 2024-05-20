package vadl.types;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a relation type in VADL's type system.
 * A relation type consists of a list of argument types and a return type.
 * The argument types are represented as a list of subclasses of the base class Type.
 * The return type is represented using a subclass of the base class Type.
 */
public class RelationType extends Type {

  private final List<Class<? extends Type>> argTypeClass;
  private final Class<? extends Type> resultTypeClass;

  protected RelationType(List<Class<? extends Type>> argTypes, Class<? extends Type> resultType) {
    this.argTypeClass = argTypes;
    this.resultTypeClass = resultType;
  }

  public List<Class<? extends Type>> argTypeClasses() {
    return argTypeClass;
  }

  public Class<? extends Type> resultTypeClass() {
    return resultTypeClass;
  }

  @Override
  public String name() {
    return "("
        + argTypeClass.stream().map(Class::getSimpleName)
        .collect(Collectors.joining(", "))
        + ") -> "
        + resultTypeClass.getSimpleName();
  }
}
