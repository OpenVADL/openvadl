package vadl.iss.passes.tcgLowering;

/**
 * TcgWidth is an enumeration representing different width sizes in the context of the TCG.
 *
 * <p>This enum is utilized to denote the bit-width of variables through predefined constants.
 * It currently supports 32-bit (i32) and 64-bit (i64) widths.
 */
@SuppressWarnings("TypeName")
public enum Tcg_32_64 {
  i32(32),
  i64(64);

  public final int width;

  Tcg_32_64(int width) {
    this.width = width;
  }

  /**
   * Converts a given width in bits to the corresponding TcgWidth enumeration value.
   *
   * @param width The width in bits to convert.
   * @return The corresponding TcgWidth enumeration value.
   * @throws IllegalArgumentException If the given width does not match a known TcgWidth.
   */
  public static Tcg_32_64 fromWidth(int width) {
    return switch (width) {
      case 32 -> i32;
      case 64 -> i64;
      default -> throw new IllegalArgumentException("Invalid width: " + width);
    };
  }
}
