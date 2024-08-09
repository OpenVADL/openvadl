package vadl.viam;

import com.google.errorprone.annotations.FormatMethod;
import org.jetbrains.annotations.Contract;
import vadl.utils.SourceLocation;

/**
 * An abstract VADL Definition, such as an Instruction, Format, Encoding, ...
 *
 * <p>
 * A definition is identified by an Identifier and may have a SourceLocation associated with it.
 * </p>
 */
public abstract class Definition {

  public final Identifier identifier;
  private SourceLocation sourceLocation = SourceLocation.INVALID_SOURCE_LOCATION;

  public Definition(Identifier identifier) {
    this.identifier = identifier;
  }

  public final String simpleName() {
    return identifier.name();
  }

  public SourceLocation sourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(SourceLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  public String name() {
    return identifier.simpleName();
  }

  /**
   * Verifies the definition's state and properties.
   *
   * <p>This method should be overridden by all definitions that require some
   * verification.</p>
   */
  public void verify() {

  }

  @FormatMethod
  @Contract("false, _, _-> fail")
  public void ensure(boolean condition, String message, Object... args) {
    if (condition) {
      return;
    }
    throwWithContext(message, args);
  }

  @FormatMethod
  @Contract("_, _-> fail")
  protected void throwWithContext(String message, Object... args) {
    throw new ViamError(message.formatted(args))
        .shrinkStacktrace(1)
        .addContext(this);
  }

  public abstract void accept(DefinitionVisitor visitor);

}
