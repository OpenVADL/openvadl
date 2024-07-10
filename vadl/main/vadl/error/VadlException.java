package vadl.error;

import java.util.List;
import java.util.stream.Collectors;

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
    super(errors.size() + " error(s):\n" + buildErrorMessage(errors));
    this.errors = errors;
  }

  private static String buildErrorMessage(List<VadlError> errors) {
    return errors.stream().map(VadlError::toString).collect(Collectors.joining("\n"));
  }
}
