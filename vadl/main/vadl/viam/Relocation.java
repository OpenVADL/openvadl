package vadl.viam;

import vadl.types.Type;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadRegNode;

/**
 * Represents a relocation definition in a VIAM specification.
 *
 * <p>Defined relocations can be embedded in the source code to refere to labels who's target
 * address is unknown. The assembler emits these relocations in the object file when expanding
 * pseudo instructions or sequences.
 * Relocations are used to change immediate values during link time.
 * They are needed either for optimization purposes or because the value is not known beforehand.
 * </p>
 */
public class Relocation extends Function {
  public Relocation(Identifier identifier, Parameter[] parameters, Type returnType) {
    super(identifier, parameters, returnType);
  }

  public Relocation(Identifier identifier, Parameter[] parameters, Type returnType,
                    Graph behavior) {
    super(identifier, parameters, returnType, behavior);
  }

  @Override
  public void verify() {
    super.verify();

    ensure(parameters().length == 1, "Relocations must have exactly one parameter");
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * A {@link Relocation} is relative when it references the {@link Register.Counter} which is
   * declared in {@link InstructionSetArchitecture#pc()}.
   */
  public boolean isRelative(Register.Counter pc) {
    return this.behavior().getNodes(ReadRegNode.class)
        .anyMatch(x -> x.register().equals(pc));
  }

  /**
   * A {@link Relocation} is absolute when it does not reference the {@link Register.Counter} which
   * is declared in {@link InstructionSetArchitecture#pc()}.
   */
  public boolean isAbsolute(Register.Counter pc) {
    return this.behavior().getNodes(ReadRegNode.class)
        .noneMatch(x -> x.register().equals(pc));
  }
}
