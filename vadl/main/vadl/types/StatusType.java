package vadl.types;

public class StatusType extends DataType {

  private final TupleType alias;

  protected StatusType() {
    alias = Type.tuple(
        Type.bool(),
        Type.bool(),
        Type.bool(),
        Type.bool()
    );
  }

  public TupleType asTuple() {
    return alias;
  }

  public int size() {
    return alias.size();
  }

  @Override
  public int bitWidth() {
    return alias.bitWidth();
  }

  @Override
  public String name() {
    return "Status";
  }

}
