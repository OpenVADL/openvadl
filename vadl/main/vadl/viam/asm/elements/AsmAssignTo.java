package vadl.viam.asm.elements;

/**
 * AssignTo is a super class for grammar elements that can be assigned to.
 */
public abstract class AsmAssignTo implements AsmGrammarElement {
  private final String assignToName;

  public AsmAssignTo(String assignToName) {
    this.assignToName = assignToName;
  }

  public String getAssignToName() {
    return assignToName;
  }
}
