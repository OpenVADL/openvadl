package vadl.types;

/**
 * Represents the `String` type in VADL.
 * Currently, a StringType of size 0 represents a String of any length. This
 * will probably change in the future.
 */
// TODO: Discuss size of 0 for String of any length
public class StringType extends Type {

  protected StringType() {
  }
  
  @Override
  public String name() {
    return "String";
  }
}
