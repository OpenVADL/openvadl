package vadl.viam.asm.elements;

/**
 * AssignTo is a super class for grammar elements that can be assigned to.
 */
public abstract class AsmAssignTo implements AsmGrammarElement {
  private final String assignToName;
  private final boolean isWithinRepetition;

  public AsmAssignTo(String assignToName, boolean isWithinRepetition) {
    this.assignToName = assignToName;
    this.isWithinRepetition = isWithinRepetition;
  }

  public String getAssignToName() {
    return assignToName;
  }

  public boolean getIsWithinRepetition() {
    return isWithinRepetition;
  }
}
