package vadl.viam.asm.elements;

/**
 * AssignTo is a super class for grammar elements that can be assigned to.
 */
public abstract class AssignTo {
  String assignToName;

  public AssignTo(String assignToName) {
    this.assignToName = assignToName;
  }
}
