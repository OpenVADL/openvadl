package vadl.types;

/**
 * An arbitrary sized sequence of Bits to represent anything.
 */
public class BitsType extends Type {
  public final int bitWidth;

  protected BitsType(int bitWidth) {
    this.bitWidth = bitWidth;
  }

  @Override
  public String name() {
    return "Bits<%d>".formatted(bitWidth);
  }

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

  public <T extends BitsType> T join(T... others) {
    var upperBound = this;
    for (var other : others) {
      if (upperBound.bitWidth < other.bitWidth) {
        upperBound = other;
      }
    }
    //noinspection ReassignedVariable
    return (T) upperBound;
  }

}
