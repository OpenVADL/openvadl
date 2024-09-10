package vadl.viam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * The Specification is the root of the VIAM, as it contains all other definitions of a
 * specification.
 * Note that the hierarchy of definitions is the same as in the textual VADL specification.
 * E.g., a {@link Function} could be in the global scope, which means it is directly held by the
 * Specification, or it could be defined in a
 * {@link InstructionSetArchitecture} definition, which means that
 * it is hold by the respective ISA definition instance and not by the Specification itself.
 */
public class Specification extends Definition {

  private final List<Definition> definitions = new ArrayList<>();


  public Specification(Identifier identifier) {
    super(identifier);
  }

  public Stream<Definition> definitions() {
    return definitions.stream();
  }

  /**
   * Returns the instruction set architecture of the specification.
   */
  public Optional<InstructionSetArchitecture> isa() {
    return definitions()
        .filter(InstructionSetArchitecture.class::isInstance)
        .map(InstructionSetArchitecture.class::cast)
        .findFirst();
  }
  
  /**
   * Returns all global format definitions as stream.
   */
  public Stream<Format> formats() {
    return definitions.stream()
        .filter(Format.class::isInstance)
        .map(Format.class::cast);
  }

  /**
   * Returns all registers as stream.
   */
  public Stream<Register> registers() {
    return isa()
        .map(x -> x.ownRegisters().stream())
        .orElseGet(Stream::empty)
        .map(Register.class::cast);
  }

  /**
   * Returns all register files as stream.
   */
  public Stream<RegisterFile> registerFiles() {
    return isa()
        .map(x -> x.ownRegisterFiles().stream())
        .orElseGet(Stream::empty)
        .map(RegisterFile.class::cast);
  }

  /**
   * Returns a stream of all format definitions within the specification, including the ones
   * nested within instruction set architectures.
   *
   * @return A stream of {@link Format} objects representing the format definitions.
   */
  public Stream<Format> findAllFormats() {
    var innerFormats = definitions.stream()
        .filter(InstructionSetArchitecture.class::isInstance)
        .map(InstructionSetArchitecture.class::cast)
        .flatMap(i -> i.ownFormats().stream());
    return Stream.concat(formats(), innerFormats);
  }

  public void add(Definition definition) {
    definitions.add(definition);
  }

  public void addAll(Collection<Definition> definition) {
    definitions.addAll(definition);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
