package vadl.error;

import java.util.List;

/**
 * A general exception that something went wrong with a list of errors that caused the problem.
 */
public class VadlException extends RuntimeException {
  public List<VadlError> errors;

  /**
   * Create a new general exception.
   *
   * @param errors are the reasons this error was thrown.
   */
  public VadlException(List<VadlError> errors) {
    this.errors = errors;
  }
}
