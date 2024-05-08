package vadl.types;

public class RangeType extends Type {

  private final Type subtype;

  protected RangeType(Type subtype) {
    this.subtype = subtype;
  }

  public Type subtype() {
    return subtype;
  }

  @Override
  public String name() {
    return "[ " + subtype.name() + " .. " + subtype.name() + " ]";
  }
}
