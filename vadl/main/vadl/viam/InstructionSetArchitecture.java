package vadl.viam;

import java.util.List;
import java.util.stream.Stream;

/**
 * An Instruction Set Architecture (ISA) definition of a VADL specification.
 */
public class InstructionSetArchitecture extends Definition {

  private final List<Instruction> instructions;
  private final List<Format> formats;
  private final Specification specification;

  public InstructionSetArchitecture(Identifier identifier,
                                    Specification specification,
                                    List<Format> formats,
                                    List<Instruction> instructions) {
    super(identifier);
    this.specification = specification;
    this.formats = formats;
    this.instructions = instructions;
  }

  public List<Instruction> instructions() {
    return instructions;
  }

  public Stream<Format> formats() {
    return Stream.concat(formats.stream(), specification.formats());
  }


}
