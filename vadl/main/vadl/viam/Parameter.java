package vadl.viam;

import javax.annotation.Nullable;
import vadl.types.Type;

/**
 * Represents a parameter in a VADL specification.
 */
public class Parameter extends Definition {

  private final Type type;

  // the parent of this parameter (e.g. a function definition)
  @Nullable
  private Definition parent;

  public Parameter(Identifier identifier, Type type) {
    super(identifier);
    this.type = type;
  }

  @Override
  public void verify() {
    super.verify();
    ensure(parent != null, "Parent definition is null, but should always be set after creation");
  }

  public Type type() {
    return type;
  }

  @Override
  public String toString() {
    return name() + ": " + type;
  }


  public Definition parent() {
    ensure(parent != null, "Parent definition is null but this should not happen");
    return parent;
  }

  public void setParent(@SuppressWarnings("NullableProblems") Definition parent) {
    this.parent = parent;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
