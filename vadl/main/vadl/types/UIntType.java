package vadl.types;

/**
 * An arbitrary sized unsigned integer.
 */
public class UIntType extends Type {
  public final int bitWidth;

  public UIntType(int bitWidth) {
    this.bitWidth = bitWidth;
  }

  @Override
  public String name() {
    return "UInt<%s>".formatted(bitWidth);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UIntType type = (UIntType) o;
    return bitWidth == type.bitWidth;
  }

  @Override
  public int hashCode() {
    return bitWidth;
  }
}
