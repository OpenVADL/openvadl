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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BitsType bitsType = (BitsType) o;
    return bitWidth == bitsType.bitWidth;
  }

  @Override
  public int hashCode() {
    return bitWidth;
  }
}
