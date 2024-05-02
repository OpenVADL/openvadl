package vadl.types;

public class DummyType extends Type {

  public static final DummyType INSTANCE = new DummyType();

  private DummyType() {}

  @Override
  public String name() {
    return "DummyType";
  }
}
