package vadl.viam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
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

  private final Field[] fieldEncodings;
  private final Format.Field[] nonEncodedFormatFields;

  public Encoding(Identifier identifier, Format format, Field[] fieldEncodings) {
    super(identifier);
    this.format = format;
    this.fieldEncodings = fieldEncodings;
    this.nonEncodedFormatFields = determineNoneEncodedFields(format, fieldEncodings);
  }

  public Type type() {
    return format.type();
  }

  public Field[] fieldEncodings() {
    return fieldEncodings;
  }

  /**
   * Returns all non-encoded format fields.
   *
   * <p>This method filters the fields of the given format and returns only the fields
   * that are not present in the set of encoded fields.</p>
   */
  public Format.Field[] nonEncodedFormatFields() {
    return nonEncodedFormatFields;
  }

  public @Nullable Field fieldEncodingOf(Format.Field formatField) {
    for (Field fieldEncoding : fieldEncodings) {
      if (fieldEncoding.formatField.equals(formatField)) {
        return fieldEncoding;
      }
    }
    return null;
  }

  public Format format() {
    return format;
  }

  @Override
  public String toString() {
    return "Encoding{ " + identifier + " = {\n\t"
        + Stream.of(fieldEncodings).map(Field::toString).collect(
        Collectors.joining(",\n\t")) + " \n}}";
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Determines what format fields are not encoded by with the given fieldEncodings.
   */
  private static Format.Field[] determineNoneEncodedFields(Format format, Field[] fieldEncodings) {
    var nonEncodedFormatFields = new ArrayList<Format.Field>();
    // determine all format fields that are not encoded by this encoding
    // TODO: do not use fields().toList()
    for (Format.Field formatField : format.fields().toList()) {
      boolean found = false;
      for (Field encField : fieldEncodings) {
        if (encField.formatField.equals(formatField)) {
          found = true;
          break;
        }
      }
      if (found) {
        nonEncodedFormatFields.add(formatField);
      }
    }
    return nonEncodedFormatFields.toArray(Format.Field[]::new);
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

      this.formatField = formatField;
      this.constant = new Constant.Value(constant.value(), formatField.type());

      verify();
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
    public void verify() {
      super.verify();
      ensure(constant.type().canBeCastTo(formatField.type()),
          "Constant is of type %s, but format field is of type %s which cannot be cast implicit",
          constant.type(),
          formatField.type());
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

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }
  }

}
