package vadl.iss.passes.decode.dto;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.Definition;
import vadl.viam.Encoding;
import vadl.viam.Instruction;

/**
 * Represents a single pattern definition in the QEMU decode tree definition.
 */
public final class Pattern implements Renderable, SourceMapping {

  private final Instruction source;
  private final Format format;

  @Nullable
  private String name;

  public Pattern(Instruction source, Format format) {
    this.source = source;
    this.format = format;
  }

  @Override
  public String render(RenderContext context) {

    final var sb = new StringBuilder();
    sb.append(name);
    sb.append(" ");

    // Insert padding to align the bit patterns
    if (name != null) {
      sb.append(" ".repeat(context.maxPatternNameLength() - name.length()));
    }

    final var pattern = toBitPattern();
    sb.append(pattern);

    // Insert padding to align the format name
    sb.append(" ".repeat(context.maxPatternBitLength() - pattern.length()));

    sb.append(" @").append(format.getName());

    // Insert padding to align the assignments
    if (format.getName() != null) {
      sb.append(" ".repeat(context.maxFormatNameLength() - format.getName().length()));
    }

    // Append constant assignments
    for (Field f : format.getArgs().getFields()) {

      // If it's a 'pseudo' field (i.e. backed by a field access), and the referenced field is a
      // constant, then we also encode the constant value in the pattern. The access transformation
      // will not be applied in this case (but most probably not be needed either).
      Definition actualFieldRef = f.getSource();
      if (actualFieldRef instanceof vadl.viam.Format.FieldAccess acc) {
        actualFieldRef = acc.fieldRef();
      }

      final Definition finalFieldRef = actualFieldRef;
      final var encodedField = Arrays.stream(source.encoding().fieldEncodings())
          .filter(i -> Objects.equals(i.formatField().identifier, finalFieldRef.identifier))
          .findFirst();

      if (encodedField.isEmpty()) {
        continue;
      }

      sb.append(" ").append(f.getSource().simpleName()).append("=");
      sb.append(encodedField.get().constant().binary(""));
    }

    return sb.toString();
  }

  @Override
  public Instruction getSource() {
    return source;
  }

  public Format getFormat() {
    return format;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(@Nonnull String name) {
    this.name = name;
  }

  /**
   * Returns the bit pattern of the pattern.
   *
   * @return the bit pattern of the pattern
   */
  public String toBitPattern() {
    final var sb = new StringBuilder();

    var fixedParts = Arrays.stream(source.encoding().fieldEncodings())
        .flatMap(e -> e.formatField().bitSlice().parts().map(p -> Pair.of(e, p)))
        .sorted(Comparator.<Pair<Encoding.Field, Constant.BitSlice.Part>, Integer>comparing(
            p -> p.right().msb()).reversed())
        .iterator();

    var i = source.format().type().bitWidth() - 1;
    while (i >= 0) {
      var nextPart = fixedParts.hasNext() ? fixedParts.next() : null;
      while (i >= 0 && (nextPart == null || i > nextPart.right().msb())) {
        sb.append(".");
        i--;
      }
      if (nextPart != null) {
        sb.append(formatPart(nextPart.left(), nextPart.right()));
        i -= nextPart.right().size();
      }
    }

    return sb.toString();
  }

  private String formatPart(Encoding.Field f, Constant.BitSlice.Part p) {
    final BigInteger value = f.constant().integer();

    List<Constant.BitSlice.Part> parts = f.formatField().bitSlice().parts()
        .sorted(Comparator.comparingInt(Constant.BitSlice.Part::lsb))
        .toList();

    int i = 0;
    int offset = 0;
    while (i < parts.size() && !parts.get(i).equals(p)) {
      offset += parts.get(i).size();
      i++;
    }

    final var sb = new StringBuilder();
    for (int j = offset + p.size() - 1; j >= offset; j--) {
      sb.append(value.testBit(j) ? "1" : "0");
    }
    return sb.toString();
  }
}
