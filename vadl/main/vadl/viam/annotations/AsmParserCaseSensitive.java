package vadl.viam.annotations;

import vadl.viam.Annotation;
import vadl.viam.AssemblyDescription;

/**
 * This annotation might be set on an {@link AssemblyDescription} definition
 * to indicate whether string comparisons in the asm parser are case-sensitive.
 */
public class AsmParserCaseSensitive extends Annotation<AssemblyDescription> {

  private final boolean isCaseSensitive;

  public AsmParserCaseSensitive(boolean isCaseSensitive) {
    this.isCaseSensitive = isCaseSensitive;
  }

  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  @Override
  public Class<AssemblyDescription> parentDefinitionClass() {
    return AssemblyDescription.class;
  }
}
