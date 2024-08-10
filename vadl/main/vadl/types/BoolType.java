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

}
