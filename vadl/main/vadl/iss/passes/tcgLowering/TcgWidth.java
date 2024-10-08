package vadl.iss.passes.tcgLowering;

/**
 * TcgWidth is an enumeration representing different width sizes in the context of the TCG.
 *
 * <p>This enum is utilized to denote the bit-width of variables through predefined constants.
 * It currently supports 32-bit (i32) and 64-bit (i64) widths.
 */
public enum TcgWidth {
  i32(32),
  i64(64);

  public final int width;

  TcgWidth(int width) {
    this.width = width;
  }
}
