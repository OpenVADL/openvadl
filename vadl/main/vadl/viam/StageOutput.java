package vadl.viam;

import vadl.types.Type;

/**
 * Stage output definition in MiA description.
 *
 * <p>A stage output belongs to a stage is written by WriteStageOutputNodes (part of the
 * stage's behavior) and read by ReadStageOutputNodes (also in other stage's behaviors).
 */
public class StageOutput extends Definition implements DefProp.WithType {

  private final Type type;

  public StageOutput(Identifier identifier, Type type) {
    super(identifier);
    this.type = type;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + type;
  }
}
