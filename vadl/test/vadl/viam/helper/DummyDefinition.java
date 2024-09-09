package vadl.viam.helper;

import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;

public class DummyDefinition extends Definition {


  public DummyDefinition() {
    super(Identifier.noLocation("JustADummyDefinition"));
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    // do nothing
  }
}
