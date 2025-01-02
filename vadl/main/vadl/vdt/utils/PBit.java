package vadl.vdt.utils;

public class PBit implements BitWise<PBit> {

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
  public PBit and(PBit other) {
    return null;
  }

  @Override
  public PBit or(PBit other) {
    return null;
  }

  @Override
  public PBit xor(PBit other) {
    return null;
  }

  @Override
  public PBit not() {
    return null;
  }

}
