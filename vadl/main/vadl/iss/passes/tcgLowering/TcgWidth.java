package vadl.iss.passes.tcgLowering;

public enum TcgWidth {
  i32(32),
  i64(64);

  public final int width;

  TcgWidth(int width) {
    this.width = width;
  }
}
