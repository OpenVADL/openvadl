package vadl.viam;

import vadl.viam.graph.Graph;

/**
 * The VADL ISA Instruction definition.
 */
public class Instruction extends Definition {

  private final Graph behaviour;
  private final Assembly assembly;
  private final Encoding encoding;

  private final Format format;

  public Instruction(
      Identifier identifier,
      Format format,
      Graph behaviour,
      Assembly assembly,
      Encoding encoding
  ) {
    super(identifier);
    this.format = format;
    this.behaviour = behaviour;
    this.assembly = assembly;
    this.encoding = encoding;
    setSourceLocation(behaviour.sourceLocation()
        .join(assembly.sourceLocation())
        .join(encoding.sourceLocation())
    );
  }

  public Graph behaviour() {
    return behaviour;
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
