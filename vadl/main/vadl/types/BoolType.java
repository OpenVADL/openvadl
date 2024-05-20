package vadl.types;

/**
 * A Boolean which can only hold true/false.
 */
public class BoolType extends DataType {

  protected BoolType() {
  }

  @Override
  public String name() {
    return "Bool";
  }

  @Override
  public int bitWidth() {
    return 1;
  }

  @Override
  public boolean canBeCastTo(DataType other) {
    if (this == other) {
      return true;
    }

    // Bool ==> Bits<1>
    if (other.getClass() == BoolType.class) {
      return other.bitWidth() == 1;
    }

    // as Bool can be cast to Bits<1>
    // all Bits<1> casting rules apply to Bool
    // TODO: Check if this is valid
    return Type.bits(1).canBeCastTo(other);
  }
}
