package vadl.pass;

/**
 * This the typed name of an identifier of a {@link Pass}.
 * Note that the difference between {@link PassName} and {@link PassKey} is that {@link PassKey}
 * must be unique in the {@link PassManager}. However, it should be possible to schedule the same
 * {@link Pass} with the same {@link PassName} multiple times.
 */
public record PassKey(String value) {

  public static PassKey of(String value) {
    return new PassKey(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
