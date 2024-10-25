package vadl.iss.passes.tcgLowering;

public enum Tcg_8_16_32_64 {
  i8(8),
  i16(16),
  i32(32),
  i64(64);

  public final int width;

  Tcg_8_16_32_64(int width) {
    this.width = width;
  }

  public static Tcg_8_16_32_64 fromWidth(int width) {
    return switch (width) {
      case 8 -> i8;
      case 16 -> i16;
      case 32 -> i32;
      case 64 -> i64;
      default -> throw new IllegalArgumentException("Invalid width: " + width);
    };
  }

}
