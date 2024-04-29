package vadl.types;

import javax.annotation.Nullable;

/**
 * A AliasType which is a link from a name to the actual concrete type.
 */
public class AliasType extends Type {
  public String alias;

  @Nullable
  public Type destination;


  public AliasType(String alias) {
    this.alias = alias;
  }

  public AliasType(String alias, Type destination) {
    this.alias = alias;
    this.destination = destination;
  }

  @Override
  public String name() {
    return "Alias<%s, %s>".formatted(alias, destination);
  }

  @Override
  public Type concreteType() {
    if (destination == null) {
      throw new RuntimeException(
          "Internal error: Somehow an unresolved alias made it to point it shouldn't");
    }

    return destination.concreteType();
  }
}
