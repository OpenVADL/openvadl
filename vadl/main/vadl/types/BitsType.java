package vadl.types;

/**
 * An arbitrary sized sequence of Bits to represent anything.
 */
public class BitsType extends DataType {
  public final int bitWidth;

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
  public <T extends BitsType> T meet(T... others) {
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
  public <T extends BitsType> T join(T... others) {
    var upperBound = this;
    for (var other : others) {
      if (upperBound.bitWidth < other.bitWidth) {
        upperBound = other;
      }
    }
    //noinspection Variable,unchecked
    return (T) upperBound;
  }

}
