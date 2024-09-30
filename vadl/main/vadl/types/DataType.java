package vadl.types;

import org.jetbrains.annotations.Nullable;

/**
 * A type that represents actual data that can be stored in a continues
 * array of bits. All data types have a bit-width in memory.
 *
 * @see BitsType
 * @see BoolType
 */
public abstract class DataType extends Type {

  public abstract int bitWidth();

  /**
   * Checks if this type can be trivially cast to another type,
   * such that the bit representation must not be changed in any way.
   *
   * @param other the type to potentially cast to.
   * @return true if it is possible, false otherwise
   */
  @Override
  public final boolean isTrivialCastTo(Type other) {
    if (other instanceof DataType otherDataType) {
      var sameLength = otherDataType.bitWidth() == bitWidth();
      return sameLength && (other instanceof BitsType || other instanceof BoolType);
    }
    return false;
  }

  public boolean isSigned() {
    return false;
  }

  @Nullable
  public abstract DataType fittingCppType();

}
