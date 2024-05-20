package vadl.types;

/**
 * An arbitrary sized unsigned integer.
 */
public class UIntType extends BitsType {

  protected UIntType(int bitWidth) {
    super(bitWidth);
  }

  @Override
  public String name() {
    return "UInt<%s>".formatted(bitWidth);
  }

  @Override
  public boolean canBeCastTo(DataType other) {
    if (this == other) {
      return true;
    }

    // UInt<N> ==> UInt<M> | N <= M
    if (other.getClass() == UIntType.class) {
      return bitWidth <= other.bitWidth();
    }

    // UInt<N> ==> Bits<M> | N <= M
    // getClass instead of instanceof is important!
    // we don't allow casting to SInt
    if (other.getClass() == BitsType.class) {
      return bitWidth <= other.bitWidth();
    }

    // as UInt<N> can be casted to Bits<N>
    // all Bits<N> casting rules apply to UInt<N>
    // TODO: Check if this is valid
    return super.canBeCastTo(other);
  }
}
