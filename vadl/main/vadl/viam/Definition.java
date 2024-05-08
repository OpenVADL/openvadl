package vadl.viam;

import com.google.errorprone.annotations.FormatMethod;
import vadl.utils.SourceLocation;

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

  @FormatMethod
  protected void ensure(boolean condition, String message, Object... args) {
    if (condition) {
      return;
    }
    throw new ViamError(message.formatted(args))
        .shrinkStacktrace(1)
        .addContext("name", this.identifier.name())
        .addContext("definition", this.toString())
        .addContext("sourceLocation", sourceLocation.toConciseString())
        .addContext("sourceCode", sourceLocation.toSourceString());
  }

}
