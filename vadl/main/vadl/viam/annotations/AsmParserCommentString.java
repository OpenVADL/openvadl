package vadl.viam.annotations;

import vadl.viam.Annotation;
import vadl.viam.AssemblyDescription;

/**
 * This annotation might be set on an {@link AssemblyDescription} definition
 * to set a character that initiates a comment in the assembly language.
 */
public class AsmParserCommentString extends Annotation<AssemblyDescription> {

  private final String commentString;

  public AsmParserCommentString(String commentString) {
    this.commentString = commentString;
  }

  public String getCommentString() {
    return commentString;
  }

  @Override
  public Class<AssemblyDescription> parentDefinitionClass() {
    return AssemblyDescription.class;
  }
}
