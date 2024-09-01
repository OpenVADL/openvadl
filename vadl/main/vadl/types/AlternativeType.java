package vadl.types;

import java.util.Set;
import java.util.stream.Collectors;

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
