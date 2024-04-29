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
  public Type concreteType() {
    return this;
  }
}
