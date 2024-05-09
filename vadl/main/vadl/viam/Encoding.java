package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.BitsType;
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
        + ", type=" + type
        + ", fields=[\n" + fields.stream().map(Field::toString).collect(Collectors.joining(",\n"))
        + "\n]}";
  }

  /**
   * A field within an encoding.
   * Holds information about the type, ranges, and value of the field.
   */
  public static class Field extends Definition {

    private final BitsType type;
    private final Constant.BitSlice bitSlice;
    private @Nullable Constant value;

    private final Encoding encoding;

    /**
     * Constructs a Field object with the given identifier, type, ranges, and encoding.
     *
     * @param identifier the identifier of the field
     * @param type       the type of the field
     * @param bitSlice   the constant bitslice of the instruction for this field
     * @param encoding   the parent encoding of the field
     */
    public Field(
        Identifier identifier,
        BitsType type,
        Constant.BitSlice bitSlice,
        Encoding encoding) {
      super(identifier);

      ensure(bitSlice.size() == type.bitWidth,
          "Field type width of %s is different to slice size of %s", type.bitWidth,
          bitSlice.size());

      this.type = type;
      this.bitSlice = bitSlice;
      this.encoding = encoding;
    }

    /**
     * Sets the statically known value of this encoding field.
     *
     * <p>The type of the constant value must match the type this field.
     */
    public void setValue(Constant value) {
      ensure(value.type().equals(type), "Value must be of type %s, was %s", type,
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

    public Constant.BitSlice bitSlice() {
      return bitSlice;
    }

    public Type type() {
      return type;
    }

    public Encoding encoding() {
      return encoding;
    }

    public int size() {
      return bitSlice.size();
    }

    @Override
    public String toString() {
      return "Field{"
          + "name=" + identifier
          + ", type=" + type
          + ", bitSlice=" + bitSlice
          + ", value=" + value
          + ", encoding=" + encoding.identifier
          + '}';
    }
  }

}
