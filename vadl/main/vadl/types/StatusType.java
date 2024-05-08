package vadl.types;

public class StatusType extends Type {

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
  public String name() {
    return "Status";
  }
}
