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

  
}
