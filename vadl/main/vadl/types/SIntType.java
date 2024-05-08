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


}
