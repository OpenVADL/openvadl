package vadl.viam.asm.elements;

/**
 * Represents the assignment of a value to an attribute in a grammar rule.
 */
public class AsmAssignToAttribute extends AsmAssignTo {

  public AsmAssignToAttribute(String assignToName, boolean isWithinRepetition) {
    super(assignToName, isWithinRepetition);
  }
}
