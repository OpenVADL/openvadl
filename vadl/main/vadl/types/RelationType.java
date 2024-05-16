package vadl.types;

import java.util.List;

/**
 * Represents a relation type in VADL's type system.
 * A relation type consists of a list of argument types and a return type.
 * The argument types are represented as a list of subclasses of the base class Type.
 * The return type is represented using a subclass of the base class Type.
 */
public class RelationType {

  private final List<Class<? extends Type>> argTypeClass;
  private final Class<? extends Type> returnTypeClass;

  protected RelationType(List<Class<? extends Type>> argTypes, Class<? extends Type> returnType) {
    this.argTypeClass = argTypes;
    this.returnTypeClass = returnType;
  }

  public List<Class<? extends Type>> argTypeClass() {
    return argTypeClass;
  }

  public Class<? extends Type> returnTypeClass() {
    return returnTypeClass;
  }
}
