package vadl.pass.exception;

import vadl.pass.PassKey;

public class DuplicatedPassKeyException extends Exception {
    public DuplicatedPassKeyException(PassKey passKey) {
        super("Pass with the key '" + passKey.value() + "' is duplicated.");
    }
}
