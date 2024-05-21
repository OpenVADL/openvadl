package vadl.types;

/**
 * Represents the `String` type in VADL.
 * Currently, a StringType of size 0 represents a String of any length. This
 * will probably change in the future.
 */
// TODO: Discuss size of 0 for String of any length
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
