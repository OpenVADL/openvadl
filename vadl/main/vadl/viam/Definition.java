package vadl.viam;

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

}
