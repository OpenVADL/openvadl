package vadl.types;

/**
 * A class that represents the VADL status type.
 *
 * <p>It is actually a tuple of size four with bool elements.
 * These four elements represent status flags:
 * <li>negative</li>
 * <li>zero</li>
 * <li>carry</li>
 * <li>overflow</li>
 * in that order.
 */
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
