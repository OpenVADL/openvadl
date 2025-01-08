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
    return Type.signedInt(bitWidth);
  }
}
