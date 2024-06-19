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
// TODO: Do we actual need this?
public class ConcreteRelationType extends Type {

  private final RelationType relationType;

  private final List<Type> argTypes;
  private final Type resultType;

  protected ConcreteRelationType(List<Type> argTypes, Type resultType) {
    this.argTypes = argTypes;
    this.resultType = resultType;

    this.relationType = Type.relation(
        argTypes.stream()
            .map(e -> (Class<? extends Type>) e.getClass())
            .collect(Collectors.toList()),
        resultType.getClass()
    );
  }

  public List<Type> argTypes() {
    return argTypes;
  }

  public Type resultType() {
    return resultType;
  }

  public RelationType relationType() {
    return relationType;
  }

  @Override
  public String name() {
    return "("
        + argTypes.stream().map(Type::name).collect(Collectors.joining(", "))
        + ") -> "
        + resultType.name();
  }
}
