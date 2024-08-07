package vadl.viam.passes.verification;

import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;

/**
 * Calls the verification method on all definitions in the given one and all its
 * member definitions.
 */
public class ViamVerifier extends DefinitionVisitor.Recursive {

  private ViamVerifier() {
  }

  public static void verifyAllIn(Definition toVerify) {
    new ViamVerifier().verifyDefinition(toVerify);
  }

  private void verifyDefinition(Definition toVerify) {
    toVerify.accept(this);
  }

  @Override
  public void afterTraversal(Definition definition) {
    super.afterTraversal(definition);
    definition.verify();
  }
}
