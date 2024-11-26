package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;

import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Tcg_8_16_32 is an enumeration representing various bit-widths used in
 * Tiny Code Generation (TCG). It supports 8-bit, 16-bit, and 32-bit widths.
 */
@SuppressWarnings("TypeName")
public enum Tcg_8_16_32 {
  i8(8),
  i16(16),
  i32(32);

  public final int width;

  Tcg_8_16_32(int width) {
    this.width = width;
  }

  /**
   * Converts a given width in bits to the corresponding Tcg_8_16_32 enumeration value.
   *
   * @param width The width in bits to convert.
   * @return The corresponding Tcg_8_16_32 enumeration value.
   * @throws IllegalArgumentException If the given width does not match a known Tcg_8_16_32
   *                                  enumeration value.
   */
  public static Tcg_8_16_32 fromWidth(int width) {
    return switch (width) {
      case 8 -> i8;
      case 16 -> i16;
      case 32 -> i32;
      default -> throw new IllegalArgumentException("Invalid width: " + width);
    };
  }

  public static Tcg_8_16_32 from(ExpressionNode expr) {
    try {
      return fromWidth(expr.type().asDataType().bitWidth());
    } catch (Exception e) {
      throw new ViamGraphError(requireNonNull(e.getMessage()), e)
          .addContext(expr);
    }
  }

}
