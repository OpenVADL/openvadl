package vadl.types;

/**
 * An arbitrary sized sequence of Bits to represent anything.
 */
public class BitsType extends Type {
  public final int bitWidth;

  public BitsType(int bitWidth) {
    this.bitWidth = bitWidth;
  }

  @Override
  public String name() {
    return "Bits<%d>".formatted(bitWidth);
  }
}
