package vadl.types;

import java.util.Collection;
import java.util.List;
import vadl.viam.Parameter;

/**
 * An arbitrary sized sequence of Bits to represent anything.
 */
public class BitsType extends DataType {
  protected final int bitWidth;

  protected BitsType(int bitWidth) {
    this.bitWidth = bitWidth;
  }

  @Override
  public int bitWidth() {
    return bitWidth;
  }

  @Override
  public String name() {
    return "Bits<%d>".formatted(bitWidth);
  }

  /**
   * Finds the meet (lower bound) of the given BitsType types
   * by returning the one with the smaller bit width.
   *
   * <p>E.g. {@code Type.bits(3).meet(Type.bits(4)) == Type.bits(3)}
   *
   * @param others the BitsType objects to find the meet of
   * @return the meet (lower bound) of the given BitsType types
   */
  @SafeVarargs
  public final <T extends BitsType> T meet(T... others) {
    var lowerBound = this;
    for (var other : others) {
      if (lowerBound.bitWidth > other.bitWidth) {
        lowerBound = other;
      }
    }
    //noinspection unchecked
    return (T) lowerBound;
  }

  /**
   * Finds the join (upper bound) for the given BitsType types
   * by returning the one with the largest bit width.
   *
   * <p>E.g. {@code Type.bits(3).meet(Type.bits(4)) == Type.bits(4)}
   *
   * @param others the BitsType objects to join
   * @param <T>    a subtype of BitsType
   * @return the BitsType object with the largest bit width
   */
  @SafeVarargs
  public final <T extends BitsType> T join(T... others) {
    return join(List.of(others));
  }

  /**
   * Finds the join (upper bound) for the given BitsType types
   * by returning the one with the largest bit width.
   *
   * <p>E.g. {@code Type.bits(3).meet(Type.bits(4)) == Type.bits(4)}
   *
   * @param others the BitsType objects to join
   * @param <T>    a subtype of BitsType
   * @return the BitsType object with the largest bit width
   */
  public final <T extends BitsType> T join(Collection<T> others) {
    var upperBound = this;
    for (var other : others) {
      if (upperBound.bitWidth < other.bitWidth) {
        upperBound = other;
      }
    }
    //noinspection Variable,unchecked
    return (T) upperBound;
  }



  @Override
  public boolean isSigned() {
    // while it is possible to auto cast bits to SInt, the BitsType is not
    // signed, as it doesn't make sense for most bits purposes
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this.getClass() == obj.getClass() && this.bitWidth == ((BitsType) obj).bitWidth;
  }

  public BitsType withBitWidth(int bitWidth) {
    return new BitsType(bitWidth);
  }
}
