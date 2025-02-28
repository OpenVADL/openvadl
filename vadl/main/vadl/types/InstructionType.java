package vadl.types;

/**
 * The Instruction type used in the micro architecture description.
 */
public class InstructionType extends MicroArchitectureType {

  protected InstructionType() {}

  @Override
  public String name() {
    return "Instruction";
  }

}
