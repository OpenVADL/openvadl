package vadl.types;

/**
 * The top type of VADL's type system.
 * All other types extend it.
 */
public abstract class Type {

  /**
   * A readable representation of the type.
   *
   * @return the name of the type
   */
  public abstract String name();

  public static BitsType bits(int bitWidth) {
    return new BitsType(bitWidth);
  }

  public static BoolType bool() {
    return new BoolType();
  }

  public static SIntType signedInt(int bitWidth) {
    return new SIntType(bitWidth);
  }

  public static UIntType unsignedInt(int bitWidth) {
    return new UIntType(bitWidth);
  }

  public static DummyType dummy() {
    return DummyType.INSTANCE;
  }

  public static RangeType range(Type subtype) {
    return new RangeType(subtype);
  }

  ;

}
