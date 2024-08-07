package vadl.types;

/**
 * An arbitrary sized signed integer.
 */
public class SIntType extends BitsType {

  protected SIntType(int bitWidth) {
    super(bitWidth);
  }

  @Override
  public String name() {
    return "SInt<%s>".formatted(bitWidth);
  }


  @Override
  public boolean canBeCastTo(DataType other) {
    if (this == other) {
      return true;
    }
    if (other.getClass() == SIntType.class) {
      return bitWidth <= ((SIntType) other).bitWidth;
    }

    // SInt<N> ==> Bits<M> | N <= M and N > 1
    // TODO: Why would N >= 1 not work?
    if (other.getClass() == BitsType.class) {
      return bitWidth <= other.bitWidth() && bitWidth >= 1;
    }

    // as SInt<N> can be casted to Bits<N>
    // all Bits<N> casting rules apply to SInt<N>
    // TODO: Check if this is valid
    return super.canBeCastTo(other);
  }

  @Override
  public boolean isSigned() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    return this.getClass() == obj.getClass()
        && super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }


  @Override
  public BitsType withBitWidth(int bitWidth) {
    return new SIntType(bitWidth);
  }
}
