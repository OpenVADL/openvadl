package vadl.viam.asm.elements;

/**
 * Represents the assignment of a value to a local variable in a grammar rule.
 */
public class AsmAssignToLocalVar extends AsmAssignTo {
  public AsmAssignToLocalVar(String assignToName, boolean isWithinRepetition) {
    super(assignToName, isWithinRepetition);
  }
}
