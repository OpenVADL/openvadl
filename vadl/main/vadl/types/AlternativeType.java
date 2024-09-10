package vadl.types;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the alternative type {@code ( type1 | type2 )}.
 */
public class AlternativeType extends Type {
  Set<Type> types;

  protected AlternativeType(Set types) {
    this.types = types;
  }

  @Override
  public String name() {
    return "(" + types.stream().map(Type::name)
        .collect(Collectors.joining(" | ")) + ")";
  }
}
