package vadl.types;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a concrete relation type in VADL's type system.
 * A concrete relation type consists of a list of argument types and a return type.
 * The argument types are represented as a list of Type objects.
 * The return type is represented using a Type object.
 *
 * <p>While the {@link RelationType} only captures the generic type classes involved,
 * the ConcreteRelationType captures the actual types including bit widths.</p>
 *
 * @see RelationType
 */
public class ConcreteRelationType {

  private final RelationType relationType;

  private final List<Type> argTypes;
  private final Type returnType;

  protected ConcreteRelationType(List<Type> argTypes, Type returnType) {
    this.argTypes = argTypes;
    this.returnType = returnType;

    this.relationType = Type.relation(
        argTypes.stream()
            .map(e -> (Class<? extends Type>) e.getClass())
            .collect(Collectors.toList()),
        returnType.getClass()
    );
  }

  public List<Type> argTypes() {
    return argTypes;
  }

  public Type returnType() {
    return returnType;
  }

  public RelationType relationType() {
    return relationType;
  }
}
