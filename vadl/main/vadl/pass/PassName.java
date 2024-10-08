package vadl.pass;

/**
 * This the typed name of a {@link Pass}.
 */
public record PassName(String value) {

  public static PassName of(String value) {
    return new PassName(value);
  }
}
