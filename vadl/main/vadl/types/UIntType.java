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

}
