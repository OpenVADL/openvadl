package vadl.types;

/**
 * An arbitrary sized unsigned integer.
 */
public class UIntType extends Type {
  public final int bitWidth;

  protected UIntType(int bitWidth) {
    this.bitWidth = bitWidth;
  }

  @Override
  public String name() {
    return "UInt<%s>".formatted(bitWidth);
  }
}
