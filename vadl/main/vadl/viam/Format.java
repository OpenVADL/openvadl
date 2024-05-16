package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * The format definition of a VADL specification.
 *
 * <p>Each field has a bit-slice and type.</p>
 */
public class Format extends Definition {

  private final Type type;
  private final List<Field> fields = new ArrayList<>();


  public Format(Identifier identifier, Type type) {
    super(identifier);
    this.type = type;
  }

  public void addField(Field field) {
    fields.add(field);
  }

  public void addFields(Field... fields) {
    this.fields.addAll(List.of(fields));
  }

  public Stream<Field> fields() {
    return fields.stream();
  }

  public Type type() {
    return type;
  }

  @Override
  public String toString() {
    return "Format{ " + identifier + ": " + type + "{\n\t"
        + fields.stream().map(Field::toString).collect(
        Collectors.joining("\n\t")) + "\n}";
  }

  /**
   * A field of a format.
   * Holds information about the type, ranges, and value of the field.
   * This is not an immediate definition field.
   */
  public static class Field extends Definition {

    private final DataType type;
    private final Constant.BitSlice bitSlice;

    private final Format format;

    /**
     * Constructs a Field object with the given identifier, type, ranges, and encoding.
     *
     * @param identifier the identifier of the field
     * @param type       the type of the field
     * @param bitSlice   the constant bitslice of the instruction for this field
     * @param format     the parent format of the field
     */
    public Field(
        Identifier identifier,
        DataType type,
        Constant.BitSlice bitSlice,
        Format format) {
      super(identifier);

      ensure(bitSlice.size() == type.bitWidth(),
          "Field type width of %s is different to slice size of %s", type.bitWidth(),
          bitSlice.size());

      this.type = type;
      this.bitSlice = bitSlice;
      this.format = format;
    }

    public Constant.BitSlice bitSlice() {
      return bitSlice;
    }

    public DataType type() {
      return type;
    }

    public Format format() {
      return format;
    }

    public int size() {
      return bitSlice.size();
    }

    @Override
    public String toString() {
      return "Field{ " + identifier + " " + bitSlice + ": " + type + " }";
    }
  }

}
