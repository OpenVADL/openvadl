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

}
