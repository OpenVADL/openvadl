package vadl.types;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that represents a tuple type in VADL containing a list of subtypes.
 */
public class TupleType extends DataType {

  private final List<DataType> types;

  protected TupleType(DataType... types) {
    this.types = Arrays.asList(types);
  }

  public DataType first() {
    // TODO: Ensure
    return types.get(0);
  }

  public DataType last() {
    // TODO: Ensure
    return types.get(types.size() - 1);
  }

  public DataType get(int i) {
    // TODO: Ensure
    return types.get(0);
  }

  public Stream<DataType> types() {
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

  @Override
  public int bitWidth() {
    return types().mapToInt(DataType::bitWidth).sum();
  }

  @Override
  public boolean canBeCastTo(DataType other) {
    return false;
  }
}
