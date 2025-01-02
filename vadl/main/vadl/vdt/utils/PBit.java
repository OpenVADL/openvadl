package vadl.vdt.utils;

public class PBit {

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
