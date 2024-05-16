package vadl.viam;

import static vadl.utils.Utils.eqs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import vadl.types.BitsType;
import vadl.types.Type;

/**
 * The encoding for a specific instruction.
 *
 * <p>It holds instruction encoded fields available to the instruction.
 * Each field has a reference to the original format field definition and
 * a constant that defines the encoding.</p>
 */
public class Encoding extends Definition {

  private final Format format;

  private final Map<Format.Field, Field> fieldEncodings = new HashMap<>();

  public Encoding(Identifier identifier, Format format) {
    super(identifier);
    this.format = format;
  }

  public Type type() {
    return format.type();
  }

  /**
   * Adds a field encoding to the encoding.
   * There must not be an encoding for this field already.
   *
   * @param fieldEncoding the field encoding to be added
   */
  public void add(Field fieldEncoding) {
    ensure(!fieldEncodings.containsKey(fieldEncoding.formatField), "Field %s already has encoding",
        fieldEncoding.identifier);
    fieldEncodings.put(fieldEncoding.formatField, fieldEncoding);
  }

  public Stream<Field> fieldEncodings() {
    return fieldEncodings.values().stream();
  }

  public Format format() {
    return format;
  }

  @Override
  public String toString() {
    return "Encoding{ " + identifier + " = {\n\t"
        + fieldEncodings.values().stream().map(Field::toString).collect(
        Collectors.joining(",\n\t")) + " \n}}";
  }


  /**
   * A field of a VADL encoding.
   * Holds information about the format field, constant value, and type of the field.
   */
  public static class Field extends Definition {

    private final Format.Field formatField;
    private final Constant.Value constant;

    /**
     * Constructs a new Field object with the given identifier, format field, and constant value.
     * The type of the constant must be implicitly cast able to the type of the format field.
     *
     * @param identifier  the identifier of the field
     * @param formatField the format field of the field
     * @param constant    the constant value of the field
     */
    public Field(Identifier identifier, Format.Field formatField, Constant.Value constant) {
      super(identifier);

      ensure(constant.type().canBeCastTo(formatField.type()),
          "Constant is of type %s, but format field is of type %s which cannot be cast implicit",
          constant.type(),
          formatField.type());

      this.formatField = formatField;
      this.constant = new Constant.Value(constant.value(), formatField.type());
    }

    public Format.Field formatField() {
      return formatField;
    }

    public Type type() {
      return formatField.type();
    }

    public Constant constant() {
      return constant;
    }

    @Override
    public String toString() {
      return "Field{ " + identifier + " = " + constant + " }";
    }

    @Override
    public int hashCode() {
      int result = formatField.hashCode();
      result = 31 * result + constant.hashCode();
      return result;
    }
  }

}
