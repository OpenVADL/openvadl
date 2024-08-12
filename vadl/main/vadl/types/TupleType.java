package vadl.types;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that represents a tuple type in VADL containing a list of subtypes.
 */
public class TupleType extends Type {

  private final List<Type> types;

  protected TupleType(Type... types) {
    this.types = Arrays.asList(types);
  }

  public Type first() {
    // TODO: Ensure
    return types.get(0);
  }

  public Type last() {
    // TODO: Ensure
    return types.get(types.size() - 1);
  }

  public Type get(int i) {
    // TODO: Ensure
    return types.get(0);
  }

  public Stream<Type> types() {
    return types.stream();
  }

  public int size() {
    return types.size();
  }

  @Override
  public String name() {
    return "(%s)".formatted(types.stream()
        .map(Type::name)
        .collect(Collectors.joining(", ")));
  }

}
