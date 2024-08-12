package vadl.viam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a VIAM VADL specification.
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
   * Returns all instruction set architectures as stream.
   */
  public Stream<InstructionSetArchitecture> isas() {
    return definitions()
        .filter(InstructionSetArchitecture.class::isInstance)
        .map(InstructionSetArchitecture.class::cast);
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
    return isas()
        .flatMap(x -> x.registers().stream())
        .map(Register.class::cast);
  }

  /**
   * Returns all register files as stream.
   */
  public Stream<RegisterFile> registerFiles() {
    return isas()
        .flatMap(x -> x.registerFiles().stream())
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
        .flatMap(i -> i.formats().stream());
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
