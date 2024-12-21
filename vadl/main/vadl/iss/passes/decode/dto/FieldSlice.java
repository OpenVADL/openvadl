package vadl.iss.passes.decode.dto;

/**
 * Represents a contiguous bit sequence in a field.
 *
 * @param start  The start bit of the slice (inclusive, lsb of the slice)
 * @param length The length of the slice
 * @param signed If the slice is signed
 */
public record FieldSlice(int start, int length, boolean signed)
    implements Renderable {

  @Override
  public String render(RenderContext context) {
    final var sb = new StringBuilder();
    sb.append(start);
    sb.append(":");
    sb.append(signed ? "s" : "");
    sb.append(length);
    return sb.toString();
  }

  @Override
  public String toString() {
    return "[" + start + ":" + length + "]";
  }
}
