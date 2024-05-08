package vadl.types;

import java.util.HashMap;
import javax.annotation.Nullable;
import javax.xml.crypto.dsig.SignedInfo;

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

  @Override
  public String toString() {
    return name();
  }

  private final static HashMap<Integer, BitsType> bitsTypes = new HashMap<>();

  public static BitsType bits(int bitWidth) {
    BitsType bitsType = bitsTypes.get(bitWidth);
    if (bitsType == null) {
      bitsType = new BitsType(bitWidth);
      bitsTypes.put(bitWidth, bitsType);
    }
    return bitsType;
  }

  private static @Nullable BoolType bool;

  public static BoolType bool() {
    if (bool == null) {
      bool = new BoolType();
    }
    return bool;
  }


  private final static HashMap<Integer, SIntType> signedIntTypes = new HashMap<>();

  public static SIntType signedInt(int bitWidth) {
    var signedIntType = signedIntTypes.get(bitWidth);
    if (signedIntType == null) {
      signedIntType = new SIntType(bitWidth);
      signedIntTypes.put(bitWidth, signedIntType);
    }
    return signedIntType;
  }

  private final static HashMap<Integer, UIntType> unsignedIntTyps = new HashMap<>();

  public static UIntType unsignedInt(int bitWidth) {
    var unsignedIntType = unsignedIntTyps.get(bitWidth);
    if (unsignedIntType == null) {
      unsignedIntType = new UIntType(bitWidth);
      unsignedIntTyps.put(bitWidth, unsignedIntType);
    }
    return unsignedIntType;
  }

  public static DummyType dummy() {
    return DummyType.INSTANCE;
  }

  private final static HashMap<Type, RangeType> rangeTypes = new HashMap<>();

  public static RangeType range(Type subtype) {
    var rangeType = rangeTypes.get(subtype);
    if (rangeType == null) {
      rangeType = new RangeType(subtype);
      rangeTypes.put(subtype, rangeType);
    }
    return rangeType;
  }

}
