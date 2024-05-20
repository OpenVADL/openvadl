package vadl.types;

public class StringType extends DataType {

  private static final int charWidth = 8;
  private final int size;

  protected StringType(int size) {
    this.size = size;
  }

  @Override
  public int bitWidth() {
    return size * charWidth;
  }

  @Override
  public boolean canBeCastTo(DataType other) {
    return this == other;
  }

  @Override
  public String name() {
    return "String<" + size + ">";
  }
}
