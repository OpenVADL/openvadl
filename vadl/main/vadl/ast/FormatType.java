package vadl.ast;

import java.util.Objects;
import vadl.types.BitsType;
import vadl.types.Type;

/**
 * A format type is a type from a format.
 * This needs to be part of the typesystem to resolve subcalls.
 *
 * <p>This type never leaves the frontend and will be lowered to the concrete datatype.
 */
class FormatType extends BitsType {
  FormatDefinition format;

  protected FormatType(FormatDefinition format) {
    super(((BitsType) Objects.requireNonNull(format.typeLiteral.type)).bitWidth());
    this.format = format;
  }

  Type innerType() {
    return Objects.requireNonNull(format.typeLiteral.type);
  }

  @Override
  public String toString() {
    return "Format<\"%s\", %s>".formatted(format.identifier().name, format.typeLiteral.type);
  }
}
