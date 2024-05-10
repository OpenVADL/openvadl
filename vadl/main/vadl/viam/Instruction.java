package vadl.viam;

import vadl.viam.graph.Graph;

/**
 * The VADL ISA Instruction definition.
 */
public class Instruction extends Definition {

  private final Graph behavior;
  private final Assembly assembly;
  private final Encoding encoding;

  private final Format format;

  /**
   * Creates an Instruction object with the given parameters.
   *
   * @param identifier The identifier of the instruction.
   * @param format     The format definition of the instruction.
   * @param behavior   The behavior graph of the instruction.
   * @param assembly   The assembly of the instruction.
   * @param encoding   The encoding of the instruction.
   */
  public Instruction(
      Identifier identifier,
      Format format,
      Graph behavior,
      Assembly assembly,
      Encoding encoding
  ) {
    super(identifier);
    this.format = format;
    this.behavior = behavior;
    this.assembly = assembly;
    this.encoding = encoding;
    setSourceLocation(behavior.sourceLocation()
        .join(assembly.sourceLocation())
        .join(encoding.sourceLocation())
    );
  }

  public Graph behavior() {
    return behavior;
  }

  public Assembly assembly() {
    return assembly;
  }

  public Encoding encoding() {
    return encoding;
  }

  public Format format() {
    return format;
  }

}
