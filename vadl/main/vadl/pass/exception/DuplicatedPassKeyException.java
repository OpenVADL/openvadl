package vadl.pass.exception;

import vadl.pass.Pass;
import vadl.pass.PassKey;

/**
 * Indicates that a {@link Pass} with the {@link PassKey} was added multiple times.
 * This is not allowed because key has to be unique.
 */
public class DuplicatedPassKeyException extends Exception {
  public DuplicatedPassKeyException(PassKey passKey) {
    super("Pass with the key '" + passKey.value() + "' is duplicated.");
  }
}
