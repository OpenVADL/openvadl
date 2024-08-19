package vadl.viam;

import java.util.List;
import vadl.viam.graph.Graph;

/**
 * The VADL ISA Instruction definition.
 */
// TODO: Instruction should have information about source and destination registers
//  (not from AST, computed by analysis).
public class Instruction extends Definition implements DefProp.WithBehavior {

  private final Graph behavior;
  private final Assembly assembly;
  private final Encoding encoding;

  /**
   * Creates an Instruction object with the given parameters.
   *
   * @param identifier The identifier of the instruction.
   * @param behavior   The behavior graph of the instruction.
   * @param assembly   The assembly of the instruction.
   * @param encoding   The encoding of the instruction.
   */
  public Instruction(
      Identifier identifier,
      Graph behavior,
      Assembly assembly,
      Encoding encoding
  ) {
    super(identifier);
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
    return encoding.format();
  }

  @Override
  public void verify() {
    super.verify();

    behavior.verify();
  }

  @Override
  public String toString() {
    return identifier.name() + ": " + format().identifier.name();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public List<Graph> behaviors() {
    return List.of(behavior);
  }
}
