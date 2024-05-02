package vadl.types;

/**
 * A dummy type for places where the correct type cannot be determined yet.
 */
public class DummyType extends Type {

  public static final DummyType INSTANCE = new DummyType();

  private DummyType() {}

  @Override
  public String name() {
    return "DummyType";
  }
}
