package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;

import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents an enumeration of bit widths used in TCG (Tiny Code Generation).
 * This enum encapsulates common bit widths (8, 16, 32, 64) typically seen in memory operations.
 */
@SuppressWarnings("TypeName")
public enum Tcg_8_16_32_64 {
  i8(8),
  i16(16),
  i32(32),
  i64(64);

  public final int width;

  Tcg_8_16_32_64(int width) {
    this.width = width;
  }

  /**
   * Converts the given bit width to its corresponding Tcg_8_16_32_64 enum value.
   *
   * @param width the bit width to convert (must be 8, 16, 32, or 64)
   * @return the corresponding Tcg_8_16_32_64 enum value
   * @throws IllegalArgumentException if the width is not 8, 16, 32, or 64
   */
  public static Tcg_8_16_32_64 fromWidth(int width) {
    return switch (width) {
      case 8 -> i8;
      case 16 -> i16;
      case 32 -> i32;
      case 64 -> i64;
      default -> throw new IllegalArgumentException("Invalid width: " + width);
    };
  }

  /**
   * Returns the width of the expressions result type.
   */
  public static Tcg_8_16_32_64 from(ExpressionNode expr) {
    try {
      return fromWidth(expr.type().asDataType().bitWidth());
    } catch (Exception e) {
      throw new ViamGraphError(requireNonNull(e.getMessage()), e)
          .addContext(expr);
    }
  }


}
