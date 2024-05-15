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
 * a constant that defines the encoding</p>
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

  public static class Field extends Definition {

    private final Format.Field formatField;
    private final Constant.Value constant;

    Field(Format.Field formatField, Constant.Value constant) {
      super(formatField.identifier);

      ensure(constant.type() == formatField.type(),
          "Constant is of type %s, but format field is of type %s", constant, formatField.type());

      this.formatField = formatField;
      this.constant = constant;
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
