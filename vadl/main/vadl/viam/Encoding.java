package vadl.viam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.Type;

/**
 * The encoding for a specific instruction.
 *
 * <p>It holds instruction encoded fields available to the instruction.
 * Each field has a bit range and type that specifies the location.
 * Optionally a field also has a constant value if it is known in advance.</p>
 */
public class Encoding extends Definition {

  private final Type type;
  private final List<Field> fields = new ArrayList<Field>();

  public Encoding(Identifier identifier, Type type) {
    super(identifier);
    this.type = type;
  }

  public Encoding addFields(Field... encodingFields) {
    fields.addAll(List.of(encodingFields));
    return this;
  }

  public Type type() {
    return type;
  }

  @Override
  public String toString() {
    return "Encoding{"
        + "name=" + identifier
        + "type=" + type
        + ", fields=[\n" + fields.stream().map(Field::toString).collect(Collectors.joining(",\n"))
        + "\n]}";
  }

  /**
   * A field within an encoding.
   * Holds information about the type, ranges, and value of the field.
   */
  public static class Field extends Definition {

    private final Type type;
    private final List<Constant.Range> ranges;
    private @Nullable Constant value;

    private final Encoding encoding;

    /**
     * Constructs a Field object with the given identifier, type, ranges, and encoding.
     *
     * @param identifier the identifier of the field
     * @param type       the type of the field
     * @param ranges     the list of constant ranges
     * @param encoding   the parent encoding of the field
     */
    public Field(
        Identifier identifier,
        Type type,
        List<Constant.Range> ranges,
        Encoding encoding) {
      super(identifier);

      ViamError.ensure(ranges.stream()
              .map(Constant::type)
              .collect(Collectors.toSet()).size() == 1,
          "Ranges must be of same type: %s", ranges
      );

      this.type = type;
      ranges.sort(Comparator
          .comparing(a -> ((Constant.Range) a).from().value())
          .reversed()
      );
      this.ranges = ranges;
      this.encoding = encoding;
    }

    /**
     * Constructs a Field object with the given identifier, type, ranges, and encoding.
     *
     * @param identifier the identifier of the field
     * @param type       the type of the field
     * @param range      the constant range
     * @param encoding   the parent encoding of the field
     */
    public Field(
        Identifier identifier,
        Type type,
        Constant.Range range,
        Encoding encoding) {
      this(identifier, type, List.of(range), encoding);
    }

    public void setValue(Constant value) {
      ViamError.ensure(value.type().equals(type), "Value must be of type %s, was %s", type,
          value.type());
      this.value = value;
    }

    @Nullable
    public Constant value() {
      return value;
    }

    public boolean hasValue() {
      return value != null;
    }

    public List<Constant.Range> ranges() {
      return ranges;
    }

    public Type type() {
      return type;
    }

    public Encoding encoding() {
      return encoding;
    }


    public int size() {
      return ranges
          .stream()
          .mapToInt(Constant.Range::size)
          .sum();
    }

    /**
     * Converts the occupation ranges to an array of integers.
     * The resulting array contains the indices of all bits, that are
     * occupied by this field.
     *
     * @return an array of integers representing the occupation ranges
     */
    public int[] toOccupationArray() {
      var arr = new int[size()];
      int index = 0;
      for (Constant.Range range : ranges) {
        List<Constant.Value> values = range.toList();
        for (Constant.Value value : values) {
          arr[index++] = value.value().intValue();
        }
      }
      return arr;
    }

    @Override
    public String toString() {
      return "Field{"
          + "name=" + identifier
          + "type=" + type
          + ", ranges=" + ranges
          + ", value=" + value
          + ", encoding=" + encoding.identifier
          + '}';
    }
  }

}
