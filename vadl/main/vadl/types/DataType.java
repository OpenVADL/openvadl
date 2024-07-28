package vadl.types;

/**
 * A type that represents actual data that can be stored in a continues
 * array of bits. All data types have a bit-width in memory.
 *
 * @see BitsType
 * @see BoolType
 * @see TupleType
 */
public abstract class DataType extends Type {

  public abstract int bitWidth();

  /**
   * Checks if this type can be implicitly cast to other type.
   *
   * <p>The following implicit casting rules are defined:
   * <li>{@code Bits<1> <=> Bool}</li>
   * <li>{@code Bits<N> ==> SInt<M> | N = M}</li>
   * <li>{@code Bits<N> ==> Bits<M> | N <= M}</li>
   * <li>{@code UInt<N> ==> Bits<M> | N <= M}</li>
   * <li>{@code SInt<N> ==> Bits<M> | N <= M}</li>
   * </p>
   *
   * @param other target cast type
   * @return true if this can be cast to other implicitly, false otherwise
   */
  public abstract boolean canBeCastTo(DataType other);

  /**
   * Checks if this type can be implicitly cast to other type.
   *
   * <p>The following implicit casting rules are defined:
   * <li>{@code Bits<1> <=> Bool}</li>
   * <li>{@code Bits<N> ==> SInt<M> | N = M}</li>
   * <li>{@code Bits<N> ==> Bits<M> | N <= M}</li>
   * <li>{@code UInt<N> ==> Bits<M> | N <= M}</li>
   * <li>{@code SInt<N> ==> Bits<M> | N <= M}</li>
   * </p>
   *
   * @param other target cast type
   * @return true if this can be cast to other implicitly, false otherwise
   */
  public final boolean canBeCastTo(Type other) {
    if (other instanceof DataType) {
      return canBeCastTo((DataType) other);
    }
    return false;
  }

  public final boolean isSigned() {
    return !(this instanceof UIntType);
  }

}
