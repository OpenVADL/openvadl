package vadl.types;

/**
 * An arbitrary sized signed integer.
 */
public class SIntType extends Type {
  public final int bitWidth;

  public SIntType(int bitWidth) {
    this.bitWidth = bitWidth;
  }

  @Override
  public String name() {
    return "SInt<%s>".formatted(bitWidth);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SIntType type = (SIntType) o;
    return bitWidth == type.bitWidth;
  }

  @Override
  public int hashCode() {
    return bitWidth;
  }
}
