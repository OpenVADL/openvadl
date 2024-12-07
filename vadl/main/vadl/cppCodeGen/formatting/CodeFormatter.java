package vadl.cppCodeGen.formatting;

import java.nio.file.Path;

/**
 * Interface for a generic code formatter that formats generated code emitted during
 * code generation.
 */
public interface CodeFormatter {

  /**
   * An exception thrown if the formatter is not available (e.g. if not installed).s
   */
  class NotAvailableException extends Exception {
    public NotAvailableException(String message) {
      super(message);
    }
  }

  /**
   * An exception thrown the formatting failed.
   */
  class FormatFailureException extends Exception {
    public FormatFailureException(String message, Throwable cause) {
      super(message, cause);
    }

    public FormatFailureException(String message) {
      super(message);
    }
  }

  /**
   * Returns if the formatter is available and can be used.
   */
  boolean isAvailable();

  /**
   * Formats the given file in place, such that the file is overwritten with the
   * formatted version.
   *
   * @param file the file to be formatted (must exist)
   * @throws NotAvailableException  if the formatter is not available
   * @throws FormatFailureException if something went wrong during formatting
   */
  void format(Path file) throws NotAvailableException, FormatFailureException;

}
