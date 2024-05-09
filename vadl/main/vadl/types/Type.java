package vadl.types;

import java.util.Arrays;
import java.util.HashMap;
import javax.annotation.Nullable;

/**
 * The top type of VADL's type system.
 * All other types extend it.
 *
 * <p>It provides static methods to retrieve the instances of all
 * concrete types. So to retrieve a type, those methods must be used,
 * such that there is only a single instance per type.</p>
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

  private static final HashMap<Integer, BitsType> bitsTypes = new HashMap<>();

  /**
   * Retrieves the BitsType instance with the specified bit width.
   *
   * @param bitWidth the bit width of the BitsType object
   * @return the BitsType object with the specified bit width
   */
  public static BitsType bits(int bitWidth) {
    BitsType bitsType = bitsTypes.get(bitWidth);
    if (bitsType == null) {
      bitsType = new BitsType(bitWidth);
      bitsTypes.put(bitWidth, bitsType);
    }
    return bitsType;
  }

  private static @Nullable BoolType bool;

  /**
   * Retrieves the instance of the BoolType.
   *
   * @return an instance of BoolType
   */
  public static BoolType bool() {
    if (bool == null) {
      bool = new BoolType();
    }
    return bool;
  }


  private static final HashMap<Integer, SIntType> signedIntTypes = new HashMap<>();

  /**
   * Retrieves the instance of SIntType with the specified bit width.
   *
   * @param bitWidth the bit width of the SIntType object
   * @return the SIntType object with the specified bit width
   */
  public static SIntType signedInt(int bitWidth) {
    var signedIntType = signedIntTypes.get(bitWidth);
    if (signedIntType == null) {
      signedIntType = new SIntType(bitWidth);
      signedIntTypes.put(bitWidth, signedIntType);
    }
    return signedIntType;
  }

  private static final HashMap<Integer, UIntType> unsignedIntTyps = new HashMap<>();

  /**
   * Retrieves the instance of UIntType with the specified bit width.
   *
   * @param bitWidth the bit width of the UIntType object
   * @return the UIntType object with the specified bit width
   */
  public static UIntType unsignedInt(int bitWidth) {
    var unsignedIntType = unsignedIntTyps.get(bitWidth);
    if (unsignedIntType == null) {
      unsignedIntType = new UIntType(bitWidth);
      unsignedIntTyps.put(bitWidth, unsignedIntType);
    }
    return unsignedIntType;
  }

  /**
   * Returns a DummyType object.
   *
   * @return a DummyType object representing a placeholder type
   */
  // TODO: Remove
  public static DummyType dummy() {
    return DummyType.INSTANCE;
  }

  private static @Nullable BitSliceType bitSliceType = null;

  /**
   * Retrieves the instance of BitSliceType.
   *
   * @return the instance of BitSliceType
   */
  public static BitSliceType bitSlice() {
    if (bitSliceType == null) {
      bitSliceType = new BitSliceType();
    }
    return bitSliceType;
  }

  private static final HashMap<Integer, TupleType> tupleTypes = new HashMap<>();

  /**
   * Retrieves the tuple type with the specified subtypes.
   *
   * @param types the subtypes of the tuple
   * @return the tuple type with the specified subtypes
   */
  public static TupleType tuple(DataType... types) {
    var hashCode = Arrays.hashCode(types);
    var tupleType = tupleTypes.get(hashCode);
    if (tupleType == null) {
      tupleType = new TupleType(types);
      tupleTypes.put(hashCode, tupleType);
    }
    return tupleType;
  }

  private static @Nullable StatusType statusType = null;

  /**
   * Retrieves the status type instance.
   *
   * @return the status type instance
   */
  public static StatusType status() {
    if (statusType == null) {
      statusType = new StatusType();
    }
    return statusType;
  }
}
