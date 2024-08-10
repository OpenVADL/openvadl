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

  /**
   * Returns a signed integer with the same {@code bitWidth}.
   */
  public SIntType makeSigned() {
    return new SIntType(bitWidth);
  }

  @Override
  public boolean isSigned() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    return this.getClass() == obj.getClass()
        && super.equals(obj);
  }

  @Override
  public BitsType withBitWidth(int bitWidth) {
    return new UIntType(bitWidth);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
