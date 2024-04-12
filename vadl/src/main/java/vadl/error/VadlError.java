package vadl.error;

import javax.annotation.Nullable;
import vadl.ast.Location;

/**
 * A common error instance in the vadl system.
 * The error itself is not an exception and cannot be thrown on its own, mostly because at many
 * times vadl tries to collect multiple errors at once to provide the users with more information.
 *
 */
public class VadlError {
  public String reason;
  public Location location;
  public @Nullable String description;
  public @Nullable String tip;

  /**
   * Create a common error.
   *
   * @param reason is a short reason for what went wrong
   * @param location a location in the source file.
   * @param description is an optional longer explanation for why the error occurred
   * @param tip is an optional tip on how the programmer might resolve this issue
   */
  public VadlError(String reason, Location location, @Nullable String description,
                   @Nullable String tip) {
    this.reason = reason;
    this.location = location;
    this.description = description;
    this.tip = tip;
  }
}
