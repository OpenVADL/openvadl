package vadl.viam;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.InlineMe;
import org.jetbrains.annotations.Contract;
import vadl.utils.SourceLocation;
import vadl.viam.graph.Graph;

/**
 * An abstract VADL Definition, such as an {@link Instruction}, {@link Format}, {@link Encoding},...
 *
 * <p>
 * A definition is identified by an Identifier and may have a SourceLocation associated with it.
 * As creator of a definition, the source location should be set where possible.
 * Use the {@link #ensure(boolean, String, Object...)} method to check for conditions that
 * must be met. It will throw an error with the definition as context if the condition is not true.
 * </p>
 */
public abstract class Definition {

  public final Identifier identifier;
  private SourceLocation sourceLocation = SourceLocation.INVALID_SOURCE_LOCATION;

  public Definition(Identifier identifier) {
    this.identifier = identifier;
  }

  public SourceLocation sourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(SourceLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  public String simpleName() {
    return identifier.simpleName();
  }

  /**
   * This is only used because some templates rely on it.
   *
   * @deprecated use {@link #simpleName()} instead
   */
  @Deprecated
  @InlineMe(replacement = "identifier.simpleName()")
  public final String name() {
    return identifier.simpleName();
  }

  /**
   * Verifies the definition's state and properties.
   *
   * <p>This method should be overridden by all definitions that require some
   * verification. The implementation should ensure that thinks like type consistency and other
   * properties are given. If the definition contains a behavior,
   * it should call {@link Graph#verify()} in its own verify method.
   * It is called by the {@link vadl.viam.passes.verification.ViamVerifier} during the
   * {@link vadl.viam.passes.verification.ViamVerificationPass}.</p>
   *
   * @see vadl.viam.passes.verification.ViamVerificationPass
   * @see vadl.viam.passes.verification.ViamVerifier
   */
  public void verify() {

  }

  /**
   * Ensures that the condition is true. Otherwise, it will throw an error with the
   * definition's context.
   */
  @FormatMethod
  @Contract("false, _, _-> fail")
  public void ensure(boolean condition, String message, Object... args) {
    if (!condition) {
      throw new ViamError(message.formatted(args))
          .shrinkStacktrace(1)
          .addContext(this);
    }
  }

  public abstract void accept(DefinitionVisitor visitor);

}
