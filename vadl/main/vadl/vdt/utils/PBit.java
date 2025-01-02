package vadl.vdt.utils;

/**
 * A pattern bit, i.e. a bit that can be either 0, 1 or <i>don't care</i>.
 */
public class PBit {

  /**
   * The possible values of a pattern bit.
   */
  public enum Value {
    ZERO, ONE, DONT_CARE
  }

  private final Value value;

  public PBit(Value value) {
    this.value = value;
  }

  public Value getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PBit)) {
      return false;
    }
    return value == ((PBit) obj).value;
  }
}
